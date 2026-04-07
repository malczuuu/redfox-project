package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.app.Application
import io.github.malczuuu.redfox.app.domain.UserRepository
import io.github.malczuuu.redfox.app.security.OAuth2Service
import io.github.malczuuu.redfox.app.security.TokenDto
import io.github.problem4j.core.Problem
import io.github.problem4j.core.ProblemException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.client.ExchangeResult
import org.springframework.test.web.servlet.client.RestTestClient

@ActiveProfiles(profiles = ["test"])
@AutoConfigureRestTestClient
@ContainerTest
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class OAuth2ControllerTests : PostgresAwareTest {

  @Autowired private lateinit var restClient: RestTestClient
  @Autowired private lateinit var userRepository: UserRepository

  @MockitoBean private lateinit var oauth2Service: OAuth2Service

  @BeforeEach
  fun beforeEach() {
    userRepository.deleteAll()
  }

  @Nested
  inner class TokenExchangeTests {

    @Test
    fun givenValidCode_whenExchangeToken_thenReturns204AndSetsTokenCookies() {
      `when`(oauth2Service.exchangeToken("auth-code", "http://localhost/cb", "verifier-xyz"))
          .thenReturn(
              TokenDto(accessToken = "acc-token", refreshToken = "ref-token", expiresIn = 120)
          )

      val requestBody =
          mapOf(
              "code" to "auth-code",
              "redirectUri" to "http://localhost/cb",
              "codeVerifier" to "verifier-xyz",
          )

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/api/oauth2/token")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
      val setCookies = response.responseHeaders[HttpHeaders.SET_COOKIE] ?: emptyList<String>()
      assertThat(setCookies.any { it.startsWith("redfox_access_token=acc-token") }).isTrue()
      assertThat(setCookies.any { it.startsWith("redfox_refresh_token=ref-token") }).isTrue()
    }

    @Test
    fun givenNoRefreshTokenInResponse_whenExchangeToken_thenReturns204WithOnlyAccessCookie() {
      `when`(oauth2Service.exchangeToken("auth-code", "http://localhost/cb", "verifier-xyz"))
          .thenReturn(TokenDto(accessToken = "acc-token", refreshToken = null, expiresIn = 120))

      val requestBody =
          mapOf(
              "code" to "auth-code",
              "redirectUri" to "http://localhost/cb",
              "codeVerifier" to "verifier-xyz",
          )

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/api/oauth2/token")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
      val setCookies = response.responseHeaders[HttpHeaders.SET_COOKIE] ?: emptyList<String>()
      assertThat(setCookies.any { it.startsWith("redfox_access_token=acc-token") }).isTrue()
      assertThat(setCookies.none { it.startsWith("redfox_refresh_token=") }).isTrue()
    }

    @Test
    fun givenAuthServerError_whenExchangeToken_thenReturns401() {
      `when`(oauth2Service.exchangeToken("bad-code", "http://localhost/cb", "verifier-xyz"))
          .thenThrow(ProblemException(Problem.of(HttpStatus.UNAUTHORIZED.value())))

      val requestBody =
          mapOf(
              "code" to "bad-code",
              "redirectUri" to "http://localhost/cb",
              "codeVerifier" to "verifier-xyz",
          )

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/api/oauth2/token")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
    }
  }

  @Nested
  inner class TokenRefreshTests {

    @Test
    fun givenValidRefreshCookie_whenRefreshToken_thenReturns204AndSetsNewCookies() {
      `when`(oauth2Service.refreshToken("valid-refresh"))
          .thenReturn(TokenDto(accessToken = "new-acc", refreshToken = "new-ref", expiresIn = 120))

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/api/oauth2/refresh")
              .header(HttpHeaders.COOKIE, "redfox_refresh_token=valid-refresh")
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
      val setCookies = response.responseHeaders[HttpHeaders.SET_COOKIE] ?: emptyList<String>()
      assertThat(setCookies.any { it.startsWith("redfox_access_token=new-acc") }).isTrue()
      assertThat(setCookies.any { it.startsWith("redfox_refresh_token=new-ref") }).isTrue()
    }

    @Test
    fun givenMissingRefreshCookie_whenRefreshToken_thenReturns401() {
      val response: ExchangeResult =
          restClient.post().uri("/api/oauth2/refresh").exchange().returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
    }

    @Test
    fun givenAuthServerError_whenRefreshToken_thenReturns401() {
      `when`(oauth2Service.refreshToken("expired-refresh"))
          .thenThrow(ProblemException(Problem.of(HttpStatus.UNAUTHORIZED.value())))

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/api/oauth2/refresh")
              .header(HttpHeaders.COOKIE, "redfox_refresh_token=expired-refresh")
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
    }
  }

  @Nested
  inner class LogoutTests {

    @Test
    fun whenLogout_thenReturns204AndClearsBothCookies() {
      val response: ExchangeResult =
          restClient.post().uri("/api/oauth2/logout").exchange().returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)

      val cookies = response.responseCookies

      assertThat(cookies["redfox_access_token"]).isNotNull.anySatisfy {
        assertThat(it.value).isEmpty()
        assertThat(it.maxAge).isZero
      }

      assertThat(cookies["redfox_refresh_token"]).isNotNull.anySatisfy {
        assertThat(it.value).isEmpty()
        assertThat(it.maxAge).isZero
      }
    }
  }
}
