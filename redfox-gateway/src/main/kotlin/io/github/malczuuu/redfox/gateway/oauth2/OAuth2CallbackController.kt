package io.github.malczuuu.redfox.gateway.oauth2

import io.github.malczuuu.redfox.gateway.config.GatewayProperties
import io.github.malczuuu.redfox.gateway.security.CookieCrypto
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

@Controller
class OAuth2CallbackController(
    private val properties: GatewayProperties,
    private val cookieCrypto: CookieCrypto,
    private val restClient: RestClient,
    private val jsonMapper: JsonMapper,
) {

  @GetMapping("/oauth2/callback")
  fun callback(
      @RequestParam("code") code: String,
      request: HttpServletRequest,
      response: HttpServletResponse,
  ) {
    val codeVerifier = extractCodeVerifier(request)
    clearPkceCookie(response)

    val tokenResponse = exchangeCode(code, codeVerifier)
    val tree = jsonMapper.readTree(tokenResponse)

    val accessToken = tree.get("access_token").textValue()
    val refreshToken = tree.get("refresh_token")?.textValue()
    val expiresIn = tree.get("expires_in")?.intValue() ?: 1800

    val accessCookie = Cookie("gw_access", cookieCrypto.encrypt(accessToken))
    accessCookie.path = "/"
    accessCookie.isHttpOnly = true
    accessCookie.maxAge = expiresIn
    response.addCookie(accessCookie)

    if (refreshToken != null) {
      val contextCookie = Cookie("gw_context", cookieCrypto.encrypt(refreshToken))
      contextCookie.path = "/"
      contextCookie.isHttpOnly = true
      contextCookie.maxAge = 28800
      response.addCookie(contextCookie)
    }

    response.sendRedirect("/")
  }

  private fun exchangeCode(code: String, codeVerifier: String?): String {
    val params = LinkedMultiValueMap<String, String>()
    params.add("grant_type", "authorization_code")
    params.add("code", code)
    params.add("redirect_uri", properties.oauth2.redirectUri)
    if (codeVerifier != null) {
      params.add("code_verifier", codeVerifier)
    }

    val credentials =
        java.util.Base64.getEncoder()
            .encodeToString(
                "${properties.oauth2.clientId}:${properties.oauth2.clientSecret}"
                    .toByteArray(Charsets.UTF_8)
            )

    return restClient
        .post()
        .uri("${properties.oauth2.authserverUrl}/oauth2/token")
        .header(HttpHeaders.AUTHORIZATION, "Basic $credentials")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(params)
        .retrieve()
        .body(String::class.java)!!
  }

  private fun extractCodeVerifier(request: HttpServletRequest): String? {
    val pkceCookie = request.cookies?.firstOrNull { it.name == "gw_pkce" } ?: return null
    return try {
      cookieCrypto.decrypt(pkceCookie.value)
    } catch (e: Exception) {
      null
    }
  }

  private fun clearPkceCookie(response: HttpServletResponse) {
    val cookie = Cookie("gw_pkce", "")
    cookie.path = "/"
    cookie.maxAge = 0
    cookie.isHttpOnly = true
    response.addCookie(cookie)
  }
}
