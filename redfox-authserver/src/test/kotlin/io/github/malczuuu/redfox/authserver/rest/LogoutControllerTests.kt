package io.github.malczuuu.redfox.authserver.rest

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.authserver.AuthServerApplication
import java.net.http.HttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestClient

@ActiveProfiles(profiles = ["test"])
@AutoConfigureRestTestClient
@ContainerTest
@SpringBootTest(classes = [AuthServerApplication::class], webEnvironment = RANDOM_PORT)
class LogoutControllerTests : PostgresAwareTest {

  @LocalServerPort private var port: Int = 0

  private lateinit var restClient: RestClient

  @BeforeEach
  fun beforeEach() {
    val httpClient: HttpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
    restClient =
        RestClient.builder()
            .baseUrl("http://localhost:$port")
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .build()
  }

  @Test
  fun givenNoRedirectUri_whenLogout_thenReturns302AndRedirectsToLoginLogout() {
    val response = restClient.get().uri("/api/logout").retrieve().toBodilessEntity()

    assertThat(response.statusCode).isEqualTo(HttpStatus.FOUND)
    assertThat(response.headers[HttpHeaders.LOCATION]).anySatisfy {
      assertThat(it).endsWith("/login?logout")
    }
  }

  @Test
  fun givenRedirectUri_whenLogout_thenReturns302AndRedirectsToRedirectUri() {
    val response =
        restClient
            .get()
            .uri("/api/logout?redirect_uri=http://localhost/cb")
            .retrieve()
            .toBodilessEntity()

    assertThat(response.statusCode).isEqualTo(HttpStatus.FOUND)
    assertThat(response.headers[HttpHeaders.LOCATION]).containsExactly("http://localhost/cb")
  }
}
