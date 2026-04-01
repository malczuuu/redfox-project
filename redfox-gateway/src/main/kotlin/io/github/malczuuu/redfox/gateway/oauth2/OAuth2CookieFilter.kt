package io.github.malczuuu.redfox.gateway.oauth2

import io.github.malczuuu.redfox.gateway.config.GatewayProperties
import io.github.malczuuu.redfox.gateway.security.CookieCrypto
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Collections
import java.util.Enumeration
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.json.JsonMapper

class OAuth2CookieFilter(
    private val properties: GatewayProperties,
    private val cookieCrypto: CookieCrypto,
    private val restClient: RestClient,
    private val jsonMapper: JsonMapper,
) : OncePerRequestFilter() {

  private val log = LoggerFactory.getLogger(OAuth2CookieFilter::class.java)

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      filterChain: FilterChain,
  ) {
    val path = request.requestURI
    if (
        path.startsWith("/oauth2/callback") ||
            path.startsWith("/logout") ||
            path.startsWith("/actuator")
    ) {
      filterChain.doFilter(request, response)
      return
    }

    val accessCookie = findCookie(request, "gw_access")

    if (accessCookie == null) {
      redirectToLogin(request, response)
      return
    }

    val accessToken =
        try {
          cookieCrypto.decrypt(accessCookie.value)
        } catch (e: Exception) {
          log.debug("Failed to decrypt gw_access cookie, redirecting to login", e)
          clearCookies(response)
          redirectToLogin(request, response)
          return
        }

    val refreshedToken = refreshIfNeeded(accessToken, request, response)
    val effectiveToken = refreshedToken ?: accessToken

    val wrappedRequest = BearerTokenRequestWrapper(request, effectiveToken)
    filterChain.doFilter(wrappedRequest, response)
  }

  private fun refreshIfNeeded(
      accessToken: String,
      request: HttpServletRequest,
      response: HttpServletResponse,
  ): String? {
    try {
      val parts = accessToken.split(".")
      if (parts.size != 3) return null

      val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
      val tree = jsonMapper.readTree(payload)
      val exp = tree.get("exp")?.longValue() ?: return null
      val iat = tree.get("iat")?.longValue() ?: return null

      val now = System.currentTimeMillis() / 1000
      val ttl = exp - iat
      val elapsed = now - iat
      val threshold = (ttl * 0.75).toLong()

      if (elapsed < threshold) return null

      val contextCookie = findCookie(request, "gw_context") ?: return null
      val refreshToken =
          try {
            cookieCrypto.decrypt(contextCookie.value)
          } catch (e: Exception) {
            log.debug("Failed to decrypt gw_context cookie", e)
            return null
          }

      return doRefresh(refreshToken, response)
    } catch (e: Exception) {
      log.debug("Token refresh check failed", e)
      return null
    }
  }

  private fun doRefresh(refreshToken: String, response: HttpServletResponse): String? {
    try {
      val params = LinkedMultiValueMap<String, String>()
      params.add("grant_type", "refresh_token")
      params.add("refresh_token", refreshToken)

      val credentials =
          Base64.getEncoder()
              .encodeToString(
                  "${properties.oauth2.clientId}:${properties.oauth2.clientSecret}"
                      .toByteArray(Charsets.UTF_8)
              )

      val body =
          restClient
              .post()
              .uri("${properties.oauth2.authserverUrl}/oauth2/token")
              .header(HttpHeaders.AUTHORIZATION, "Basic $credentials")
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(params)
              .retrieve()
              .body(String::class.java) ?: return null

      val tree = jsonMapper.readTree(body)
      val newAccessToken = tree.get("access_token").textValue()
      val newRefreshToken = tree.get("refresh_token")?.textValue()
      val expiresIn = tree.get("expires_in")?.intValue() ?: 1800

      val newAccessCookie = Cookie("gw_access", cookieCrypto.encrypt(newAccessToken))
      newAccessCookie.path = "/"
      newAccessCookie.isHttpOnly = true
      newAccessCookie.maxAge = expiresIn
      response.addCookie(newAccessCookie)

      if (newRefreshToken != null) {
        val newContextCookie = Cookie("gw_context", cookieCrypto.encrypt(newRefreshToken))
        newContextCookie.path = "/"
        newContextCookie.isHttpOnly = true
        newContextCookie.maxAge = 28800
        response.addCookie(newContextCookie)
      }

      return newAccessToken
    } catch (e: Exception) {
      log.debug("Token refresh failed", e)
      return null
    }
  }

  private fun redirectToLogin(request: HttpServletRequest, response: HttpServletResponse) {
    val codeVerifier = generateCodeVerifier()
    val codeChallenge = generateCodeChallenge(codeVerifier)

    val pkecCookie = Cookie("gw_pkce", cookieCrypto.encrypt(codeVerifier))
    pkecCookie.path = "/"
    pkecCookie.isHttpOnly = true
    pkecCookie.maxAge = 300
    response.addCookie(pkecCookie)

    val authUrl = buildString {
      append(properties.oauth2.authserverUrl)
      append("/oauth2/authorize")
      append("?response_type=code")
      append("&client_id=")
      append(URLEncoder.encode(properties.oauth2.clientId, Charsets.UTF_8))
      append("&redirect_uri=")
      append(URLEncoder.encode(properties.oauth2.redirectUri, Charsets.UTF_8))
      append("&scope=openid")
      append("&code_challenge=")
      append(URLEncoder.encode(codeChallenge, Charsets.UTF_8))
      append("&code_challenge_method=S256")
    }
    response.sendRedirect(authUrl)
  }

  private fun generateCodeVerifier(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }

  private fun generateCodeChallenge(verifier: String): String {
    val digest =
        MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  private fun clearCookies(response: HttpServletResponse) {
    val accessCookie = Cookie("gw_access", "")
    accessCookie.path = "/"
    accessCookie.maxAge = 0
    response.addCookie(accessCookie)

    val contextCookie = Cookie("gw_context", "")
    contextCookie.path = "/"
    contextCookie.maxAge = 0
    response.addCookie(contextCookie)
  }

  private fun findCookie(request: HttpServletRequest, name: String): Cookie? {
    return request.cookies?.firstOrNull { it.name == name }
  }

  private class BearerTokenRequestWrapper(
      request: HttpServletRequest,
      private val token: String,
  ) : HttpServletRequestWrapper(request) {

    override fun getHeader(name: String): String? {
      if (HttpHeaders.AUTHORIZATION.equals(name, ignoreCase = true)) {
        return "Bearer $token"
      }
      return super.getHeader(name)
    }

    override fun getHeaders(name: String): Enumeration<String> {
      if (HttpHeaders.AUTHORIZATION.equals(name, ignoreCase = true)) {
        return Collections.enumeration(listOf("Bearer $token"))
      }
      return super.getHeaders(name)
    }

    override fun getHeaderNames(): Enumeration<String> {
      val names = super.getHeaderNames().toList().toMutableList()
      if (names.none { it.equals(HttpHeaders.AUTHORIZATION, ignoreCase = true) }) {
        names.add(HttpHeaders.AUTHORIZATION)
      }
      return Collections.enumeration(names)
    }
  }
}
