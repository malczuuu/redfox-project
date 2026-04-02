package io.github.malczuuu.redfox.app.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Instant
import java.util.Base64
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class TokenRefreshFilter(
    private val authProperties: AuthProperties,
    private val jsonMapper: JsonMapper,
) : OncePerRequestFilter() {

  private val restClient = RestClient.create()

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      filterChain: FilterChain,
  ) {
    val accessToken =
        request.cookies
            ?.firstOrNull { it.name == CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE }
            ?.value
    val refreshToken = request.cookies?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE }?.value

    if (accessToken != null && isExpired(accessToken) && refreshToken != null) {
      try {
        val tokenResponse = refreshTokens(refreshToken)
        val newAccessToken = tokenResponse.get("access_token").stringValue()
        setTokenCookies(tokenResponse, response)
        request.setAttribute(REFRESHED_TOKEN_ATTR, newAccessToken)
      } catch (_: Exception) {}
    }

    filterChain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return !request.requestURI.startsWith("/api/v1/")
  }

  private fun isExpired(jwt: String): Boolean {
    val parts = jwt.split(".")
    if (parts.size != 3) return true
    return try {
      val payload = Base64.getUrlDecoder().decode(parts[1])
      val claims = jsonMapper.readTree(payload)
      val exp = claims.get("exp")?.asLong() ?: return true
      Instant.ofEpochSecond(exp).isBefore(Instant.now())
    } catch (_: Exception) {
      true
    }
  }

  private fun refreshTokens(refreshToken: String): JsonNode {
    val formData = LinkedMultiValueMap<String, String>()
    formData.add("grant_type", "refresh_token")
    formData.add("refresh_token", refreshToken)
    formData.add("client_id", authProperties.clientId)

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
    val newRefreshToken = tokenResponse.get("refresh_token")?.stringValue()
    val expiresIn = tokenResponse.get("expires_in")?.asInt() ?: 1800

    val accessCookie = Cookie(CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE, accessToken)
    accessCookie.isHttpOnly = true
    accessCookie.secure = authProperties.cookieSecure
    accessCookie.path = "/api"
    accessCookie.maxAge = expiresIn
    accessCookie.setAttribute("SameSite", "Lax")
    response.addCookie(accessCookie)

    if (newRefreshToken != null) {
      val refreshCookie = Cookie(REFRESH_TOKEN_COOKIE, newRefreshToken)
      refreshCookie.isHttpOnly = true
      refreshCookie.secure = authProperties.cookieSecure
      refreshCookie.path = "/api/oauth2"
      refreshCookie.maxAge = tokenResponse.get("refresh_token_expires_in")?.asInt() ?: (8 * 3600)
      refreshCookie.setAttribute("SameSite", "Lax")
      response.addCookie(refreshCookie)
    }
  }

  companion object {
    const val REFRESHED_TOKEN_ATTR = "redfox_refreshed_access_token"
    private const val REFRESH_TOKEN_COOKIE = "redfox_refresh_token"
  }
}
