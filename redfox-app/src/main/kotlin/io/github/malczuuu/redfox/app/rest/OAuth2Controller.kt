package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.security.AuthProperties
import io.github.malczuuu.redfox.app.security.CookieBearerTokenResolver
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@RestController
@RequestMapping("/api/oauth2")
class OAuth2Controller(
    private val authProperties: AuthProperties,
    private val jsonMapper: JsonMapper,
) {

  private val restClient = RestClient.create()

  @PostMapping("/token")
  fun exchangeToken(
      @RequestBody request: TokenExchangeRequest,
      response: HttpServletResponse,
  ): ResponseEntity<Void> {
    val formData = LinkedMultiValueMap<String, String>()
    formData.add("grant_type", "authorization_code")
    formData.add("code", request.code)
    formData.add("redirect_uri", request.redirectUri)
    formData.add("client_id", authProperties.clientId)
    formData.add("code_verifier", request.codeVerifier)

    val tokenResponse = callTokenEndpoint(formData)
    setTokenCookies(tokenResponse, response)
    return ResponseEntity.noContent().build()
  }

  @PostMapping("/refresh")
  fun refreshToken(
      request: HttpServletRequest,
      response: HttpServletResponse,
  ): ResponseEntity<Void> {
    val refreshToken =
        request.cookies?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE }?.value
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

    val formData = LinkedMultiValueMap<String, String>()
    formData.add("grant_type", "refresh_token")
    formData.add("refresh_token", refreshToken)
    formData.add("client_id", authProperties.clientId)

    val tokenResponse = callTokenEndpoint(formData)
    setTokenCookies(tokenResponse, response)
    return ResponseEntity.noContent().build()
  }

  @PostMapping("/logout")
  fun logout(response: HttpServletResponse): ResponseEntity<Void> {
    clearCookie(response, CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE)
    clearCookie(response, REFRESH_TOKEN_COOKIE)
    return ResponseEntity.noContent().build()
  }

  private fun callTokenEndpoint(formData: LinkedMultiValueMap<String, String>): JsonNode {
    val body =
        restClient
            .post()
            .uri(authProperties.authserverTokenUri)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body(formData)
            .retrieve()
            .body(String::class.java)!!
    return jsonMapper.readTree(body)
  }

  private fun setTokenCookies(tokenResponse: JsonNode, response: HttpServletResponse) {
    val accessToken = tokenResponse.get("access_token").stringValue()
    val refreshToken = tokenResponse.get("refresh_token")?.stringValue()
    val expiresIn = tokenResponse.get("expires_in")?.asInt() ?: 1800

    val accessCookie = Cookie(CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE, accessToken)
    accessCookie.isHttpOnly = true
    accessCookie.secure = authProperties.cookieSecure
    accessCookie.path = "/api"
    accessCookie.maxAge = expiresIn
    accessCookie.setAttribute("SameSite", "Lax")
    response.addCookie(accessCookie)

    if (refreshToken != null) {
      val refreshCookie = Cookie(REFRESH_TOKEN_COOKIE, refreshToken)
      refreshCookie.isHttpOnly = true
      refreshCookie.secure = authProperties.cookieSecure
      refreshCookie.path = "/api/oauth2"
      refreshCookie.maxAge = 8 * 3600
      refreshCookie.setAttribute("SameSite", "Lax")
      response.addCookie(refreshCookie)
    }
  }

  private fun clearCookie(response: HttpServletResponse, name: String) {
    val cookie = Cookie(name, "")
    cookie.isHttpOnly = true
    cookie.secure = authProperties.cookieSecure
    cookie.path = if (name == REFRESH_TOKEN_COOKIE) "/api/oauth2" else "/api"
    cookie.maxAge = 0
    cookie.setAttribute("SameSite", "Lax")
    response.addCookie(cookie)
  }

  companion object {
    private const val REFRESH_TOKEN_COOKIE = "redfox_refresh_token"
  }
}

data class TokenExchangeRequest(
    val code: String,
    val redirectUri: String,
    val codeVerifier: String,
)
