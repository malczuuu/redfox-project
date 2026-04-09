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
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

@ActiveProfiles(profiles = ["test"])
@AutoConfigureRestTestClient
@ContainerTest
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class AuthControllerTests : PostgresAwareTest {

  @Autowired private lateinit var restClient: RestTestClient
  @Autowired private lateinit var userRepository: UserRepository
  @Autowired private lateinit var jsonMapper: JsonMapper

  @MockitoBean private lateinit var oauth2Service: OAuth2Service

  @BeforeEach
  fun beforeEach() {
    userRepository.deleteAll()
  }

  @Nested
  inner class TokenExchangeTests {

    @Test
    fun givenValidCode_whenExchangeToken_thenReturns200AndSetsTokenCookies() {
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
              .uri("/auth/token")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      val setCookies = response.responseHeaders[HttpHeaders.SET_COOKIE] ?: emptyList<String>()
      assertThat(setCookies.any { it.startsWith("redfox_refresh_token=ref-token") }).isTrue()
      assertThat(response.responseCookies["redfox_xsrf_token"]).isNotNull.anySatisfy { cookie ->
        assertThat(cookie.value).isNotEmpty()
      }
    }

    @Test
    fun givenNoRefreshTokenInResponse_whenExchangeToken_thenReturns200WithNoRefreshCookie() {
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
              .uri("/auth/token")
              .contentType(APPLICATION_JSON)
              .body(requestBody)
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      val setCookies = response.responseHeaders[HttpHeaders.SET_COOKIE] ?: emptyList<String>()
      assertThat(setCookies.none { it.startsWith("redfox_refresh_token=") }).isTrue()
      assertThat(response.responseCookies["redfox_xsrf_token"]).isNotNull.anySatisfy { cookie ->
        assertThat(cookie.value).isNotEmpty()
      }
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
              .uri("/auth/token")
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
    fun givenValidRefreshCookie_whenRefreshToken_thenReturns200AndSetsNewCookies() {
      `when`(oauth2Service.refreshToken("valid-refresh"))
          .thenReturn(TokenDto(accessToken = "new-acc", refreshToken = "new-ref", expiresIn = 120))

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/auth/refresh")
              .header(
                  HttpHeaders.COOKIE,
                  "redfox_refresh_token=valid-refresh; redfox_xsrf_token=test-xsrf-token-value",
              )
              .header("X-Xsrf-Token", "test-xsrf-token-value")
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      val setCookies = response.responseHeaders[HttpHeaders.SET_COOKIE] ?: emptyList<String>()
      assertThat(setCookies.any { it.startsWith("redfox_refresh_token=new-ref") }).isTrue()
      assertThat(response.responseCookies["redfox_xsrf_token"]).isNotNull.anySatisfy { cookie ->
        assertThat(cookie.value).isNotEmpty()
      }
    }

    @Test
    fun givenMissingRefreshCookie_whenRefreshToken_thenReturns401() {
      val response: ExchangeResult =
          restClient
              .post()
              .uri("/auth/refresh")
              .header(HttpHeaders.COOKIE, "redfox_xsrf_token=test-xsrf-token-value")
              .header("X-Xsrf-Token", "test-xsrf-token-value")
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
    }

    @Test
    fun givenMissingXsrfToken_whenRefreshToken_thenReturns400WithDetail() {
      val response: ExchangeResult =
          restClient
              .post()
              .uri("/auth/refresh")
              .header(HttpHeaders.COOKIE, "redfox_refresh_token=valid-refresh")
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("xsrf verification failed")
                  .build()
          )
    }

    @Test
    fun givenMismatchedXsrfTokens_whenRefreshToken_thenReturns400WithDetail() {
      val response: ExchangeResult =
          restClient
              .post()
              .uri("/auth/refresh")
              .header(
                  HttpHeaders.COOKIE,
                  "redfox_refresh_token=valid-refresh; redfox_xsrf_token=correct-token",
              )
              .header("X-Xsrf-Token", "wrong-token")
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
      val problem = jsonMapper.readValue<Problem>(response.responseBodyContent)
      assertThat(problem)
          .isEqualTo(
              Problem.builder()
                  .status(HttpStatus.BAD_REQUEST.value())
                  .detail("xsrf verification failed")
                  .build()
          )
    }

    @Test
    fun givenAuthServerError_whenRefreshToken_thenReturns401() {
      `when`(oauth2Service.refreshToken("expired-refresh"))
          .thenThrow(ProblemException(Problem.of(HttpStatus.UNAUTHORIZED.value())))

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/auth/refresh")
              .header(
                  HttpHeaders.COOKIE,
                  "redfox_refresh_token=expired-refresh; redfox_xsrf_token=test-xsrf-token-value",
              )
              .header("X-Xsrf-Token", "test-xsrf-token-value")
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
      val response: ExchangeResult = restClient.post().uri("/auth/logout").exchange().returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)

      val cookies = response.responseCookies

      assertThat(cookies["redfox_refresh_token"]).isNotNull.anySatisfy {
        assertThat(it.value).isEmpty()
        assertThat(it.maxAge).isZero()
      }

      assertThat(cookies["redfox_xsrf_token"]).isNotNull.anySatisfy {
        assertThat(it.value).isEmpty()
        assertThat(it.maxAge).isZero()
      }
    }
  }
}
