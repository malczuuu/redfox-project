package io.github.malczuuu.redfox.app.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.unauthorized
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.app.Application
import io.github.malczuuu.redfox.app.domain.UserRepository
import io.github.problem4j.core.Problem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.client.ExchangeResult
import org.springframework.test.web.servlet.client.RestTestClient
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

@ActiveProfiles(profiles = ["test"])
@AutoConfigureRestTestClient
@ContainerTest
@EnableWireMock(ConfigureWireMock(name = "authserver", baseUrlProperties = ["authserver.url"]))
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class AuthControllerTests : PostgresAwareTest {

  @Autowired private lateinit var restClient: RestTestClient
  @Autowired private lateinit var userRepository: UserRepository
  @Autowired private lateinit var jsonMapper: JsonMapper

  @InjectWireMock("authserver") private lateinit var authServer: WireMockServer

  @BeforeEach
  fun beforeEach() {
    userRepository.deleteAll()
    authServer.resetAll()
  }

  @Nested
  inner class TokenExchangeTests {

    @Test
    fun givenValidCode_whenExchangeToken_thenReturns200AndSetsTokenCookies() {
      authServer.stubFor(
          post(urlEqualTo("/oauth2/token"))
              .withRequestBody(containing("grant_type=authorization_code"))
              .withRequestBody(containing("code=auth-code"))
              .willReturn(
                  okJson(
                      """{"access_token":"acc-token","refresh_token":"ref-token","expires_in":120}"""
                  )
              )
      )

      val response =
          restClient
              .post()
              .uri("/auth/token")
              .contentType(APPLICATION_JSON)
              .body(
                  mapOf(
                      "code" to "auth-code",
                      "redirectUri" to "http://localhost/cb",
                      "codeVerifier" to "verifier-xyz",
                  )
              )
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      assertThat(response.responseHeaders[HttpHeaders.SET_COOKIE]).anySatisfy {
        assertThat(it).startsWith("redfox_refresh_token=ref-token")
      }
      assertThat(response.responseCookies["redfox_xsrf_token"]).isNotNull.anySatisfy { cookie ->
        assertThat(cookie.value).isNotEmpty()
      }
    }

    @Test
    fun givenValidCode_whenExchangeToken_thenReturns200WithAccessTokenInBody() {
      authServer.stubFor(
          post(urlEqualTo("/oauth2/token"))
              .willReturn(
                  okJson(
                      """{"access_token":"acc-token","refresh_token":"ref-token","expires_in":120}"""
                  )
              )
      )

      val response =
          restClient
              .post()
              .uri("/auth/token")
              .contentType(APPLICATION_JSON)
              .body(
                  mapOf(
                      "code" to "auth-code",
                      "redirectUri" to "http://localhost/cb",
                      "codeVerifier" to "verifier-xyz",
                  )
              )
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      val body = jsonMapper.readValue<Map<String, *>>(response.responseBodyContent)
      assertThat(body["accessToken"]).isEqualTo("acc-token")
      assertThat(body["expiresIn"]).isEqualTo(120)
      assertThat(body).doesNotContainKey("refreshToken")
    }

    @Test
    fun givenNoRefreshTokenInResponse_whenExchangeToken_thenNoRefreshCookieSet() {
      authServer.stubFor(
          post(urlEqualTo("/oauth2/token"))
              .willReturn(okJson("""{"access_token":"acc-token","expires_in":120}"""))
      )

      val response =
          restClient
              .post()
              .uri("/auth/token")
              .contentType(APPLICATION_JSON)
              .body(
                  mapOf(
                      "code" to "auth-code",
                      "redirectUri" to "http://localhost/cb",
                      "codeVerifier" to "verifier-xyz",
                  )
              )
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      val setCookies = response.responseHeaders[HttpHeaders.SET_COOKIE] ?: emptyList()
      assertThat(setCookies.none { it.startsWith("redfox_refresh_token=") }).isTrue()
    }

    @Test
    fun givenAuthServerError_whenExchangeToken_thenReturns401() {
      authServer.stubFor(post(urlEqualTo("/oauth2/token")).willReturn(unauthorized()))

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/auth/token")
              .contentType(APPLICATION_JSON)
              .body(
                  mapOf(
                      "code" to "bad-code",
                      "redirectUri" to "http://localhost/cb",
                      "codeVerifier" to "verifier-xyz",
                  )
              )
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
      assertThat(response.responseHeaders.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
    }
  }

  @Nested
  inner class TokenRefreshTests {

    @Test
    fun givenValidRefreshCookie_whenRefreshToken_thenReturns200AndRotatesCookies() {
      authServer.stubFor(
          post(urlEqualTo("/oauth2/token"))
              .withRequestBody(containing("grant_type=refresh_token"))
              .withRequestBody(containing("refresh_token=valid-refresh"))
              .willReturn(
                  okJson(
                      """{"access_token":"new-acc","refresh_token":"new-ref","expires_in":120}"""
                  )
              )
      )

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/auth/refresh")
              .header(
                  HttpHeaders.COOKIE,
                  "redfox_refresh_token=valid-refresh; redfox_xsrf_token=test-xsrf-value",
              )
              .header("X-Xsrf-Token", "test-xsrf-value")
              .exchange()
              .returnResult()

      assertThat(response.status).isEqualTo(HttpStatus.OK)
      assertThat(response.responseHeaders[HttpHeaders.SET_COOKIE]).anySatisfy {
        assertThat(it).startsWith("redfox_refresh_token=new-ref")
      }
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
              .header(HttpHeaders.COOKIE, "redfox_xsrf_token=test-xsrf-value")
              .header("X-Xsrf-Token", "test-xsrf-value")
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
      authServer.stubFor(post(urlEqualTo("/oauth2/token")).willReturn(unauthorized()))

      val response: ExchangeResult =
          restClient
              .post()
              .uri("/auth/refresh")
              .header(
                  HttpHeaders.COOKIE,
                  "redfox_refresh_token=expired-refresh; redfox_xsrf_token=test-xsrf-value",
              )
              .header("X-Xsrf-Token", "test-xsrf-value")
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
