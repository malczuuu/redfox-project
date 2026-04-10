package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.app.Application
import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.core.ProjectDto
import io.github.malczuuu.redfox.app.domain.ProjectEntity
import io.github.malczuuu.redfox.app.domain.ProjectRepository
import io.github.malczuuu.redfox.app.domain.ThingEntity
import io.github.malczuuu.redfox.app.domain.ThingRepository
import io.github.malczuuu.redfox.app.domain.UserEntity
import io.github.malczuuu.redfox.app.domain.UserRepository
import io.github.malczuuu.redfox.testkit.JwtTestHelper
import io.github.malczuuu.redfox.testkit.MockJwtTest
import io.github.problem4j.core.Problem
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.client.RestTestClient
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

@ActiveProfiles(profiles = ["test"])
@AutoConfigureRestTestClient
@ContainerTest
@MockJwtTest
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class ProjectControllerTests : PostgresAwareTest {

  companion object {
    private const val PAGINATION_LIST_SIZE = 50
  }

  @Autowired private lateinit var restClient: RestTestClient
  @Autowired private lateinit var projectRepository: ProjectRepository
  @Autowired private lateinit var thingRepository: ThingRepository
  @Autowired private lateinit var userRepository: UserRepository
  @Autowired private lateinit var jsonMapper: JsonMapper

  private lateinit var project: ProjectEntity

  @BeforeEach
  fun beforeEach() {
    thingRepository.deleteAll()
    projectRepository.deleteAll()
    userRepository.deleteAll()

    project = ProjectEntity(code = "P101", name = "Test Project", description = "Test description")
    project = projectRepository.save(project)

    userRepository.save(
        UserEntity(
            login = "admin",
            passhash = "{noop}admin",
            firstName = "Admin",
            lastName = "Admin",
        )
    )

    restClient =
        restClient
            .mutate()
            .requestInterceptor { request, bytes, execution ->
              request.headers.setBearerAuth(JwtTestHelper.generateToken("admin"))
              execution.execute(request, bytes)
            }
            .build()
  }

  @Nested
  inner class ProjectQueryTests {
    @Test
    fun givenMultipleProjects_whenGetProjects_thenReturnsContent() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      val body = jsonMapper.readValue<PageResult<ProjectDto>>(response.responseBodyContent)
      assertThat(body.content).hasSize(1)
      assertThat(body.content[0].id).isEqualTo(project.id)
    }

    @Test
    fun givenNegativePage_whenGetProjects_thenReturns400() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects?page=-1&size=20")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(
                          mapOf("field" to "page", "error" to "must be greater than or equal to 0")
                      ),
                  )
                  .build()
          )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun givenInvalidSize_whenGetProjects_thenReturns400(size: Int) {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects?page=0&size=${size}")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "size", "error" to "must be greater than 0")),
                  )
                  .build()
          )
    }

    @Test
    fun givenExistingId_whenGetProject_thenReturns200AndProject() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${project.id}")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      val body = jsonMapper.readValue<ProjectDto>(response.responseBodyContent)
      assertThat(body.id).isEqualTo(project.id)
      assertThat(body.code).isEqualTo(project.code)
      assertThat(body.name).isEqualTo(project.name)
      assertThat(body.createdAt).isCloseTo(project.createdAt, within(1, ChronoUnit.MILLIS))
      assertThat(body.updatedAt).isCloseTo(project.updatedAt, within(1, ChronoUnit.MILLIS))
      assertThat(body.version).isEqualTo(project.version)
    }

    @Test
    fun givenUnknownId_whenGetProject_thenReturns404() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${UUID.randomUUID()}")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)

      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.NOT_FOUND.value())
                  .detail("project not found")
                  .build()
          )
    }

    @Test
    fun givenInvalidId_whenGetProject_thenReturns400() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/invalid-uuid")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "id")
                  .build()
          )
    }
  }

  @Nested
  inner class ProjectListPaginationTests {
    private lateinit var expectedOrderedIds: List<UUID>

    @BeforeEach
    fun beforeEach() {
      thingRepository.deleteAll()
      projectRepository.deleteAll()
      repeat(PAGINATION_LIST_SIZE) { i ->
        projectRepository.save(
            ProjectEntity(
                code = "PX${i.toString().padStart(3, '0')}",
                name = "Project $i",
                description = "D$i",
            )
        )
      }
      expectedOrderedIds = projectRepository.findAll().sortedBy { it.code }.map { it.id }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 10, 20, PAGINATION_LIST_SIZE])
    fun givenSeededProjects_whenGetProjectsWithPageSize_thenStub(pageSize: Int) {
      val collected = mutableListOf<ProjectDto>()
      var pageIndex = 0
      while (collected.size < expectedOrderedIds.size) {
        val page = fetchProjectsPage(pageIndex, pageSize)
        assertThat(page.totalElements).isEqualTo(expectedOrderedIds.size.toLong())
        collected.addAll(page.content)
        pageIndex++
      }

      assertThat(pageIndex)
          .isEqualTo(
              expectedOrderedIds.size / pageSize +
                  if (expectedOrderedIds.size % pageSize == 0) 0 else 1
          )
      assertThat(collected.map { it.id }).isEqualTo(expectedOrderedIds)
    }

    private fun fetchProjectsPage(page: Int, size: Int): PageResult<ProjectDto> {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects?page=$page&size=$size")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()
      assertThat(response.status).isEqualTo(HttpStatus.OK)
      return jsonMapper.readValue(response.responseBodyContent)
    }
  }

  @Nested
  inner class ProjectCreateTests {
    @Test
    fun givenValidBody_whenCreatingProject_thenReturns201AndLocation() {
      val requestBody = mapOf("code" to "P202", "name" to "New Project", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.CREATED)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      assertThat(response.responseHeaders.location).isNotNull()
      assertThat(response.responseHeaders.location!!.path).startsWith("/api/v1/projects/")

      val body = jsonMapper.readValue<Identity>(response.responseBodyContent)
      assertThat(body.id).isNotBlank()
    }

    @Test
    fun givenDuplicateCode_whenCreatingProject_thenReturns409() {
      val requestBody =
          mapOf("code" to project.code, "name" to "New Project", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.CONFLICT)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)

      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.CONFLICT.value())
                  .detail("project already exists")
                  .build()
          )
    }

    @Test
    fun givenNullCode_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to null, "name" to "Name", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "code")
                  .build()
          )
    }

    @Test
    fun givenBlankCode_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to "", "name" to "Name", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "code", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenCodeTooLong_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to "a".repeat(256), "name" to "Name", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "code", "error" to "size must be between 0 and 255")),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullName_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to "P202", "name" to null, "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "name")
                  .build()
          )
    }

    @Test
    fun givenBlankName_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to "P202", "name" to "", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "name", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenNameTooLong_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to "P202", "name" to "a".repeat(256), "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "name", "error" to "size must be between 0 and 255")),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullDescription_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to "P202", "name" to "Name", "description" to null)

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "description")
                  .build()
          )
    }

    @Test
    fun givenBlankDescription_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to "P202", "name" to "Name", "description" to "")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "description", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenDescriptionTooLong_whenCreatingProject_thenReturns400() {
      val requestBody = mapOf("code" to "P202", "name" to "Name", "description" to "a".repeat(2049))

      val response =
          restClient
              .post()
              .uri("/api/v1/projects")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(
                          mapOf(
                              "field" to "description",
                              "error" to "size must be between 0 and 2048",
                          )
                      ),
                  )
                  .build()
          )
    }
  }

  @Nested
  inner class ProjectUpdateTests {
    @Test
    fun givenValidBody_whenUpdateProject_thenReturns204AndUpdatesProject() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)

      val updated = projectRepository.findOne(project.id).orElseThrow()
      assertThat(updated.name).isEqualTo("Updated Name")
    }

    @Test
    fun givenInvalidId_whenUpdateProject_thenReturns400() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/invalid-uuid")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "id")
                  .build()
          )
    }

    @Test
    fun givenNullName_whenUpdateProject_thenReturns400() {
      val requestBody = mapOf("name" to null, "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "name")
                  .build()
          )
    }

    @Test
    fun givenBlankName_whenUpdateProject_thenReturns400() {
      val requestBody = mapOf("name" to "", "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "name", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenNameTooLong_whenUpdateProject_thenReturns400() {
      val requestBody =
          mapOf("name" to "a".repeat(256), "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "name", "error" to "size must be between 0 and 255")),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullDescription_whenUpdateProject_thenReturns400() {
      val requestBody = mapOf("name" to "Updated Name", "description" to null, "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "description")
                  .build()
          )
    }

    @Test
    fun givenBlankDescription_whenUpdateProject_thenReturns400() {
      val requestBody = mapOf("name" to "Updated Name", "description" to "", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(mapOf("field" to "description", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenDescriptionTooLong_whenUpdateProject_thenReturns400() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "a".repeat(2049), "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("validation failed")
                  .extension(
                      "errors",
                      listOf(
                          mapOf(
                              "field" to "description",
                              "error" to "size must be between 0 and 2048",
                          )
                      ),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullVersion_whenUpdateProject_thenReturns400() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "Updated Desc", "version" to null)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "version")
                  .extension("kind", "integer")
                  .build()
          )
    }

    @Test
    fun givenUnknownId_whenUpdateProject_thenReturns404() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${UUID.randomUUID()}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.NOT_FOUND.value())
                  .detail("project not found")
                  .build()
          )
    }
  }

  @Nested
  inner class ProjectDeleteTests {
    @Test
    fun givenExistingProject_whenDelete_thenReturns204AndProjectIsSoftDeleted() {
      val response =
          restClient.delete().uri("/api/v1/projects/${project.id}").exchange().returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
      val deleted = projectRepository.findById(project.id).orElseThrow()
      assertThat(deleted.deletedAt).isNotNull()
    }

    @Test
    fun givenInvalidId_whenDeleteProject_thenReturns400() {
      val response =
          restClient
              .delete()
              .uri("/api/v1/projects/invalid-uuid")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("type mismatch")
                  .extension("property", "id")
                  .build()
          )
    }

    @Test
    fun givenProjectWithThings_whenDeleteProject_thenThingsAreSoftDeleted() {
      val thing =
          ThingEntity(
              project = project,
              code = "T01",
              name = "Test Thing",
              description = "Test thing desc",
          )
      thingRepository.save(thing)

      val response =
          restClient.delete().uri("/api/v1/projects/${project.id}").exchange().returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
      val deletedProject = projectRepository.findById(project.id).orElseThrow()
      assertThat(deletedProject.deletedAt).isNotNull()

      val deletedThing = thingRepository.findById(thing.id).orElseThrow()
      assertThat(deletedThing.deletedAt).isNotNull()
    }
  }
}
