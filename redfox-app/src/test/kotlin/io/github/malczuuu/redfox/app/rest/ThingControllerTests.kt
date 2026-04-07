package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.app.Application
import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.core.ThingDto
import io.github.malczuuu.redfox.app.domain.ProjectEntity
import io.github.malczuuu.redfox.app.domain.ProjectRepository
import io.github.malczuuu.redfox.app.domain.ThingEntity
import io.github.malczuuu.redfox.app.domain.ThingRepository
import io.github.malczuuu.redfox.app.domain.UserEntity
import io.github.malczuuu.redfox.app.domain.UserRepository
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
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class ThingControllerTests : PostgresAwareTest {

  companion object {
    private const val PAGINATION_LIST_SIZE = 50
  }

  @Autowired private lateinit var restClient: RestTestClient
  @Autowired private lateinit var projectRepository: ProjectRepository
  @Autowired private lateinit var thingRepository: ThingRepository
  @Autowired private lateinit var userRepository: UserRepository
  @Autowired private lateinit var jsonMapper: JsonMapper

  private lateinit var project: ProjectEntity
  private lateinit var thing: ThingEntity

  @BeforeEach
  fun beforeEach() {
    thingRepository.deleteAll()
    projectRepository.deleteAll()
    userRepository.deleteAll()

    project = ProjectEntity(code = "P101", name = "Test Project", description = "Test description")
    project = projectRepository.save(project)

    thing =
        ThingEntity(
            project = project,
            code = "T01",
            name = "Test Thing",
            description = "Test thing desc",
        )
    thing = thingRepository.save(thing)

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
              request.headers.setBasicAuth("admin", "admin")
              execution.execute(request, bytes)
            }
            .build()
  }

  @Nested
  inner class ThingQueryTests {
    @Test
    fun givenMultipleThings_whenGetThings_thenReturnsContent() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${project.id}/things")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      val body = jsonMapper.readValue<PageResult<ThingDto>>(response.responseBodyContent)

      assertThat(body.content).hasSize(1)
      assertThat(body.content[0].id).isEqualTo(thing.id)
    }

    @Test
    fun givenNegativePage_whenGetThings_thenReturns400() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${project.id}/things?page=-1&size=20")
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
    fun givenInvalidSize_whenGetThings_thenReturns400(size: Int) {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${project.id}/things?page=0&size=${size}")
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
    fun givenUnknownProjectId_whenGetThings_thenReturns404AndProjectNotFound() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${UUID.randomUUID()}/things")
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
    fun givenExistingId_whenGetThing_thenReturns200AndThing() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      val body = jsonMapper.readValue<ThingDto>(response.responseBodyContent)
      assertThat(body.id).isEqualTo(thing.id)
      assertThat(body.code).isEqualTo(thing.code)
      assertThat(body.name).isEqualTo(thing.name)
      assertThat(body.createdAt).isCloseTo(thing.createdAt, within(1, ChronoUnit.MILLIS))
      assertThat(body.updatedAt).isCloseTo(thing.updatedAt, within(1, ChronoUnit.MILLIS))
      assertThat(body.version).isEqualTo(thing.version)
    }

    @Test
    fun givenUnknownId_whenGetThing_thenReturns404() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${project.id}/things/${UUID.randomUUID()}")
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
                  .detail("thing not found")
                  .build()
          )
    }

    @Test
    fun givenUnknownProjectId_whenGetThing_thenReturns404AndProjectNotFound() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${UUID.randomUUID()}/things/${thing.id}")
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
    fun givenInvalidId_whenGetThing_thenReturns400() {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${project.id}/things/invalid-uuid")
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
  inner class ThingListPaginationTests {

    private lateinit var expectedOrderedIds: List<UUID>

    @BeforeEach
    fun beforeEach() {
      thingRepository.deleteAll()
      repeat(PAGINATION_LIST_SIZE) { i ->
        thingRepository.save(
            ThingEntity(
                project = project,
                code = "TX${i.toString().padStart(3, '0')}",
                name = "Thing $i",
                description = "D$i",
            )
        )
      }
      expectedOrderedIds = thingRepository.findAll().sortedBy { it.code }.map { it.id }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 10, 20, PAGINATION_LIST_SIZE])
    fun givenSeededThings_whenGetThingsWithPageSize_thenStub(pageSize: Int) {
      val collected = mutableListOf<ThingDto>()
      var pageIndex = 0
      while (collected.size < expectedOrderedIds.size) {
        val page = fetchThingsPage(pageIndex, pageSize)
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

    private fun fetchThingsPage(page: Int, size: Int): PageResult<ThingDto> {
      val response =
          restClient
              .get()
              .uri("/api/v1/projects/${project.id}/things?page=$page&size=$size")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()
      assertThat(response.status).isEqualTo(HttpStatus.OK)
      return jsonMapper.readValue(response.responseBodyContent)
    }
  }

  @Nested
  inner class ThingCreateTests {
    @Test
    fun givenValidBody_whenCreatingThing_thenReturns201AndLocation() {
      val requestBody = mapOf("code" to "T02", "name" to "New Thing", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.CREATED)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      assertThat(response.responseHeaders.location).isNotNull()
      assertThat(response.responseHeaders.location!!.path)
          .startsWith("/api/v1/projects/${project.id}/things/")

      val body = jsonMapper.readValue<Identity>(response.responseBodyContent)
      assertThat(body.id).isNotBlank()
    }

    @Test
    fun givenDuplicateCode_whenCreatingThing_thenReturns409() {
      val requestBody = mapOf("code" to thing.code, "name" to "New Thing", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
                  .detail("thing already exists")
                  .build()
          )
    }

    @Test
    fun givenNullCode_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to null, "name" to "Name", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenBlankCode_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "", "name" to "Name", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenCodeTooLong_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "a".repeat(256), "name" to "Name", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenNullName_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "T02", "name" to null, "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenBlankName_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "T02", "name" to "", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenNameTooLong_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "T02", "name" to "a".repeat(256), "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenNullDescription_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "T02", "name" to "Name", "description" to null)

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenBlankDescription_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "T02", "name" to "Name", "description" to "")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenDescriptionTooLong_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "T02", "name" to "Name", "description" to "a".repeat(2049))

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${project.id}/things")
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
    fun givenInvalidProjectId_whenCreatingThing_thenReturns400() {
      val requestBody = mapOf("code" to "T02", "name" to "New Thing", "description" to "Desc")
      val response =
          restClient
              .post()
              .uri("/api/v1/projects/invalid-uuid/things")
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
                  .extension("property", "projectId")
                  .build()
          )
    }

    @Test
    fun givenUnknownProjectId_whenCreatingThing_thenReturns404AndProjectNotFound() {
      val requestBody = mapOf("code" to "T02", "name" to "New Thing", "description" to "Desc")

      val response =
          restClient
              .post()
              .uri("/api/v1/projects/${UUID.randomUUID()}/things")
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
  inner class ThingUpdateTests {
    @Test
    fun givenValidBody_whenUpdateThing_thenReturns204AndUpdatesThing() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)

      val updated = thingRepository.findById(thing.id).orElseThrow()
      assertThat(updated.name).isEqualTo("Updated Name")
    }

    @Test
    fun givenUnknownProjectId_whenUpdateThing_thenReturns404AndProjectNotFound() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${UUID.randomUUID()}/things/${thing.id}")
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

    @Test
    fun givenInvalidId_whenUpdateThing_thenReturns400() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/invalid-uuid")
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
    fun givenNullName_whenUpdateThing_thenReturns400() {
      val requestBody = mapOf("name" to null, "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
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
    fun givenBlankName_whenUpdateThing_thenReturns400() {
      val requestBody = mapOf("name" to "", "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
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
    fun givenNameTooLong_whenUpdateThing_thenReturns400() {
      val requestBody =
          mapOf("name" to "a".repeat(256), "description" to "Updated Desc", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
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
    fun givenNullDescription_whenUpdateThing_thenReturns400() {
      val requestBody = mapOf("name" to "Updated Name", "description" to null, "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
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
    fun givenBlankDescription_whenUpdateThing_thenReturns400() {
      val requestBody = mapOf("name" to "Updated Name", "description" to "", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
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
    fun givenDescriptionTooLong_whenUpdateThing_thenReturns400() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "a".repeat(2049), "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
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
    fun givenNullVersion_whenUpdateThing_thenReturns400() {
      val requestBody =
          mapOf("name" to "Updated Name", "description" to "Updated Desc", "version" to null)
      val response =
          restClient
              .put()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
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
  }

  @Nested
  inner class ThingDeleteTests {
    @Test
    fun givenExistingThing_whenDelete_thenReturns204AndThingIsSoftDeleted() {
      val response =
          restClient
              .delete()
              .uri("/api/v1/projects/${project.id}/things/${thing.id}")
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
      val deleted = thingRepository.findById(thing.id).orElseThrow()
      assertThat(deleted.deletedAt).isNotNull()
    }

    @Test
    fun givenUnknownProjectId_whenDeleteThing_thenReturns404AndProjectNotFound() {
      val response =
          restClient
              .delete()
              .uri("/api/v1/projects/${UUID.randomUUID()}/things/${thing.id}")
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
    fun givenInvalidId_whenDeleteThing_thenReturns400() {
      val response =
          restClient
              .delete()
              .uri("/api/v1/projects/${project.id}/things/invalid-uuid")
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
}
