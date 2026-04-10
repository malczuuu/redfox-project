package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.app.Application
import io.github.malczuuu.redfox.app.domain.UserEntity
import io.github.malczuuu.redfox.app.domain.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.client.RestTestClient
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

@ActiveProfiles(profiles = ["test"])
@AutoConfigureRestTestClient
@ContainerTest
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class WhoamiControllerTests : PostgresAwareTest {

  @Autowired private lateinit var restClient: RestTestClient
  @Autowired private lateinit var userRepository: UserRepository
  @Autowired private lateinit var jsonMapper: JsonMapper

  private lateinit var unauthenticatedClient: RestTestClient

  @BeforeEach
  fun beforeEach() {
    userRepository.deleteAll()

    userRepository.save(
        UserEntity(
            login = "admin",
            passhash = "{noop}admin",
            firstName = "Admin",
            lastName = "Admin",
        )
    )

    unauthenticatedClient = restClient
    restClient =
        restClient
            .mutate()
            .requestInterceptor { request, bytes, execution ->
              request.headers.setBasicAuth("admin", "admin")
              execution.execute(request, bytes)
            }
            .build()
  }

  @Test
  fun givenAuthenticatedUser_whenWhoami_thenReturns200WithLogin() {
    val response = restClient.get().uri("/api/v1/whoami").exchange().returnResult()

    assertThat(response.status).isEqualTo(HttpStatus.OK)
    assertThat(response.responseHeaders.contentType?.toString()).contains(APPLICATION_JSON_VALUE)
    val body = jsonMapper.readValue<Map<String, Any?>>(response.responseBodyContent)
    assertThat(body["login"]).isEqualTo("admin")
  }

  @Test
  fun givenUnauthenticatedRequest_whenWhoami_thenReturns401() {
    val response = unauthenticatedClient.get().uri("/api/v1/whoami").exchange().returnResult()

    assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    assertThat(response.responseHeaders.contentType?.isCompatibleWith(APPLICATION_PROBLEM_JSON))
        .isTrue()
  }
}
