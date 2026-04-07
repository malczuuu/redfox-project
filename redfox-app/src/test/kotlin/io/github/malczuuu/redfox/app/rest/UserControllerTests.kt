package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.app.Application
import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.core.UserDto
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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.client.ExchangeResult
import org.springframework.test.web.servlet.client.RestTestClient
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

@ActiveProfiles(profiles = ["test"])
@AutoConfigureRestTestClient
@ContainerTest
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class UserControllerTests : PostgresAwareTest {

  companion object {
    private const val PAGINATION_LIST_SIZE = 50
  }

  @Autowired private lateinit var restClient: RestTestClient
  @Autowired private lateinit var userRepository: UserRepository
  @Autowired private lateinit var passwordEncoder: PasswordEncoder
  @Autowired private lateinit var jsonMapper: JsonMapper

  private lateinit var user: UserEntity

  @BeforeEach
  fun beforeEach() {
    userRepository.deleteAll()

    user =
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
  inner class UserQueryTests {
    @Test
    fun givenExistingId_whenGetUser_thenReturns200AndUser() {
      val response: ExchangeResult =
          restClient
              .get()
              .uri("/api/v1/users/${user.id}")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      val body = jsonMapper.readValue<UserDto>(response.responseBodyContent)
      assertThat(body.id).isEqualTo(user.id)
      assertThat(body.login).isEqualTo(user.login)
      assertThat(body.firstName).isEqualTo(user.firstName)
      assertThat(body.createdAt).isCloseTo(user.createdAt, within(1, ChronoUnit.MILLIS))
      assertThat(body.updatedAt).isCloseTo(user.updatedAt, within(1, ChronoUnit.MILLIS))
      assertThat(body.version).isEqualTo(user.version)
    }

    @Test
    fun givenUnknownId_whenGetUser_thenReturns404() {
      val response =
          restClient
              .get()
              .uri("/api/v1/users/${UUID.randomUUID()}")
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
                  .detail("user not found")
                  .build()
          )
    }

    @Test
    fun givenMultipleUsers_whenGetUsers_thenReturnsContent() {
      val response =
          restClient.get().uri("/api/v1/users").accept(APPLICATION_JSON).exchange().returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      val body = jsonMapper.readValue<PageResult<UserDto>>(response.responseBodyContent)
      assertThat(body.content).hasSize(1)
      assertThat(body.content[0].id).isEqualTo(user.id)
    }

    @Test
    fun givenNegativePage_whenGetUsers_thenReturns400() {
      val response =
          restClient
              .get()
              .uri("/api/v1/users?page=-1&size=20")
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
    fun givenInvalidSize_whenGetUsers_thenReturns400(size: Int) {
      val response =
          restClient
              .get()
              .uri("/api/v1/users?page=0&size=${size}")
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
    fun givenInvalidId_whenGetUser_thenReturns400() {
      val response =
          restClient
              .get()
              .uri("/api/v1/users/invalid-uuid")
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
  inner class UserListPaginationTests {
    private lateinit var expectedOrderedIds: List<UUID>

    @BeforeEach
    fun beforeEach() {
      repeat(PAGINATION_LIST_SIZE) { i ->
        userRepository.save(
            UserEntity(
                login = "UX${i.toString().padStart(3, '0')}",
                passhash = "h",
                firstName = "F$i",
                lastName = "L",
            )
        )
      }
      expectedOrderedIds = userRepository.findAll().sortedBy { it.login }.map { it.id }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 10, 20, PAGINATION_LIST_SIZE])
    fun givenSeededUsers_whenGetUsersWithPageSize_thenStub(pageSize: Int) {
      val collected = mutableListOf<UserDto>()
      var pageIndex = 0
      while (collected.size < expectedOrderedIds.size) {
        val page = fetchUsersPage(pageIndex, pageSize)
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

    private fun fetchUsersPage(page: Int, size: Int): PageResult<UserDto> {
      val response =
          restClient
              .get()
              .uri("/api/v1/users?page=$page&size=$size")
              .accept(APPLICATION_JSON)
              .exchange()
              .returnResult()
      assertThat(response.status).isEqualTo(HttpStatus.OK)
      return jsonMapper.readValue(response.responseBodyContent)
    }
  }

  @Nested
  inner class UserCreateTests {
    @Test
    fun givenValidBody_whenCreatingUser_thenReturns201AndLocation() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "passX",
              "firstName" to "Alice",
              "lastName" to "Smith",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.CREATED)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_JSON)

      assertThat(response.responseHeaders.location).isNotNull()
      assertThat(response.responseHeaders.location!!.path).startsWith("/api/v1/users/")

      val body = jsonMapper.readValue<Identity>(response.responseBodyContent)
      assertThat(body.id).isNotBlank()
      val entity = userRepository.findById(UUID.fromString(body.id)).orElseThrow()
      assertThat(entity.login).isEqualTo("alice")
      assertThat(entity.passhash).isNotEqualTo("passX")
      assertThat(entity.passhash).matches { passwordEncoder.matches("passX", it) }
      assertThat(entity.firstName).isEqualTo("Alice")
      assertThat(entity.lastName).isEqualTo("Smith")
      assertThat(entity.version).isEqualTo(0L)
    }

    @Test
    fun givenDuplicateLogin_whenCreatingUser_thenReturns409() {
      val requestBody =
          mapOf(
              "login" to user.login,
              "password" to "pass",
              "firstName" to "John",
              "lastName" to "Doe",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                  .detail("user already exists")
                  .build()
          )
    }

    @Test
    fun givenNullLogin_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to null,
              "password" to "pass",
              "firstName" to "John",
              "lastName" to "Doe",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                  .extension("property", "login")
                  .build()
          )
    }

    @Test
    fun givenBlankLogin_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "",
              "password" to "pass",
              "firstName" to "John",
              "lastName" to "Doe",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                      listOf(mapOf("field" to "login", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenLoginTooLong_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "a".repeat(256),
              "password" to "pass",
              "firstName" to "John",
              "lastName" to "Doe",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                          mapOf("field" to "login", "error" to "size must be between 0 and 255")
                      ),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullPasshash_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to null,
              "firstName" to "Alice",
              "lastName" to "Smith",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                  .extension("property", "password")
                  .build()
          )
    }

    @Test
    fun givenBlankPasshash_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "",
              "firstName" to "Alice",
              "lastName" to "Smith",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                      listOf(mapOf("field" to "password", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenPasshashTooLong_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "a".repeat(256),
              "firstName" to "Alice",
              "lastName" to "Smith",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                          mapOf("field" to "password", "error" to "size must be between 0 and 255")
                      ),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullFirstName_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "passX",
              "firstName" to null,
              "lastName" to "Smith",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                  .extension("property", "firstName")
                  .build()
          )
    }

    @Test
    fun givenBlankFirstName_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "passX",
              "firstName" to "",
              "lastName" to "Smith",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                      listOf(mapOf("field" to "firstName", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenFirstNameTooLong_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "passX",
              "firstName" to "a".repeat(256),
              "lastName" to "Smith",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                          mapOf("field" to "firstName", "error" to "size must be between 0 and 255")
                      ),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullLastName_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "passX",
              "firstName" to "Alice",
              "lastName" to null,
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                  .extension("property", "lastName")
                  .build()
          )
    }

    @Test
    fun givenBlankLastName_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "passX",
              "firstName" to "Alice",
              "lastName" to "",
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                      listOf(mapOf("field" to "lastName", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenLastNameTooLong_whenCreatingUser_thenReturns400() {
      val requestBody =
          mapOf(
              "login" to "alice",
              "password" to "passX",
              "firstName" to "Alice",
              "lastName" to "a".repeat(256),
          )

      val response =
          restClient
              .post()
              .uri("/api/v1/users")
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
                          mapOf("field" to "lastName", "error" to "size must be between 0 and 255")
                      ),
                  )
                  .build()
          )
    }
  }

  @Nested
  inner class UserUpdateTests {
    @Test
    fun givenValidBody_whenUpdateUser_thenReturns204AndUpdatesUser() {
      val requestBody = mapOf("firstName" to "Jane", "lastName" to "Doe", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/${user.id}")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)

      val updated = userRepository.findById(user.id).orElseThrow()
      assertThat(updated.firstName).isEqualTo("Jane")
    }

    @Test
    fun givenInvalidId_whenUpdateUser_thenReturns400() {
      val requestBody = mapOf("firstName" to "Jane", "lastName" to "Doe", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/invalid-uuid")
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
    fun givenNullFirstName_whenUpdateUser_thenReturns400() {
      val requestBody = mapOf("firstName" to null, "lastName" to "Doe", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/${user.id}")
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
                  .extension("property", "firstName")
                  .build()
          )
    }

    @Test
    fun givenBlankFirstName_whenUpdateUser_thenReturns400() {
      val requestBody = mapOf("firstName" to "", "lastName" to "Doe", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/${user.id}")
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
                      listOf(mapOf("field" to "firstName", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenFirstNameTooLong_whenUpdateUser_thenReturns400() {
      val requestBody = mapOf("firstName" to "a".repeat(256), "lastName" to "Doe", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/${user.id}")
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
                          mapOf("field" to "firstName", "error" to "size must be between 0 and 255")
                      ),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullLastName_whenUpdateUser_thenReturns400() {
      val requestBody = mapOf("firstName" to "Jane", "lastName" to null, "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/${user.id}")
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
                  .extension("property", "lastName")
                  .build()
          )
    }

    @Test
    fun givenBlankLastName_whenUpdateUser_thenReturns400() {
      val requestBody = mapOf("firstName" to "Jane", "lastName" to "", "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/${user.id}")
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
                      listOf(mapOf("field" to "lastName", "error" to "must not be blank")),
                  )
                  .build()
          )
    }

    @Test
    fun givenLastNameTooLong_whenUpdateUser_thenReturns400() {
      val requestBody = mapOf("firstName" to "Jane", "lastName" to "a".repeat(256), "version" to 0L)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/${user.id}")
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
                          mapOf("field" to "lastName", "error" to "size must be between 0 and 255")
                      ),
                  )
                  .build()
          )
    }

    @Test
    fun givenNullVersion_whenUpdateUser_thenReturns400() {
      val requestBody = mapOf("firstName" to "Jane", "lastName" to "Doe", "version" to null)
      val response =
          restClient
              .put()
              .uri("/api/v1/users/${user.id}")
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
  inner class UserDeleteTests {
    @Test
    fun givenExistingUser_whenDelete_thenReturns204AndUserIsSoftDeleted() {
      val response = restClient.delete().uri("/api/v1/users/${user.id}").exchange().returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
      val deleted = userRepository.findById(user.id).orElseThrow()
      assertThat(deleted.deletedAt).isNotNull()
    }

    @Test
    fun givenInvalidId_whenDeleteUser_thenReturns400() {
      val response = restClient.delete().uri("/api/v1/users/invalid-uuid").exchange().returnResult()

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
