package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.security.AuthProperties
import io.github.malczuuu.redfox.app.security.CookieBearerTokenResolver
import io.github.malczuuu.redfox.app.security.OAuth2Service
import io.github.malczuuu.redfox.app.security.TokenDto
import io.github.malczuuu.redfox.app.security.TokenExchangeDto
import io.github.problem4j.core.Problem
import io.github.problem4j.core.ProblemException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/oauth2")
class OAuth2Controller(
    private val oauth2Service: OAuth2Service,
    private val authProperties: AuthProperties,
) {

  @PostMapping("/token")
  fun exchangeToken(
      @RequestBody request: TokenExchangeDto,
      response: HttpServletResponse,
  ): ResponseEntity<Void> {
    val tokenResponse =
        oauth2Service.exchangeToken(request.code, request.redirectUri, request.codeVerifier)
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
            ?: throw ProblemException(Problem.of(HttpStatus.UNAUTHORIZED.value()))
    val tokenResponse = oauth2Service.refreshToken(refreshToken)
    setTokenCookies(tokenResponse, response)
    return ResponseEntity.noContent().build()
  }

  @PostMapping("/logout")
  fun logout(response: HttpServletResponse): ResponseEntity<Void> {
    clearCookie(response, CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE)
    clearCookie(response, REFRESH_TOKEN_COOKIE)
    return ResponseEntity.noContent().build()
  }

  private fun setTokenCookies(token: TokenDto, response: HttpServletResponse) {
    val accessCookie = Cookie(CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE, token.accessToken)
    accessCookie.isHttpOnly = true
    accessCookie.secure = authProperties.cookieSecure
    accessCookie.path = "/"
    accessCookie.maxAge = token.expiresIn
    accessCookie.setAttribute("SameSite", "Lax")
    response.addCookie(accessCookie)

    if (token.refreshToken != null) {
      val refreshCookie = Cookie(REFRESH_TOKEN_COOKIE, token.refreshToken)
      refreshCookie.isHttpOnly = true
      refreshCookie.secure = authProperties.cookieSecure
      refreshCookie.path = "/"
      refreshCookie.maxAge = authProperties.refreshCookieMaxAge.toSeconds().toInt()
      refreshCookie.setAttribute("SameSite", "Lax")
      response.addCookie(refreshCookie)
    }
  }

  private fun clearCookie(response: HttpServletResponse, name: String) {
    val cookie = Cookie(name, "")
    cookie.isHttpOnly = true
    cookie.secure = authProperties.cookieSecure
    cookie.path = "/"
    cookie.maxAge = 0
    cookie.setAttribute("SameSite", "Lax")
    response.addCookie(cookie)
  }

  companion object {
    const val REFRESH_TOKEN_COOKIE = "redfox_refresh_token"
  }
}
