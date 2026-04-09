package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.security.AuthProperties
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
@RequestMapping("/auth")
class AuthController(
    private val oauth2Service: OAuth2Service,
    private val authProperties: AuthProperties,
) {

  companion object {
    private const val REFRESH_TOKEN_COOKIE = "redfox_refresh_token"
  }

  @PostMapping("/token")
  fun exchangeToken(
      @RequestBody request: TokenExchangeDto,
      response: HttpServletResponse,
  ): ResponseEntity<TokenDto> {
    val token = oauth2Service.exchangeToken(request.code, request.redirectUri, request.codeVerifier)
    setRefreshTokenCookie(token, response)
    return ResponseEntity.ok(token)
  }

  @PostMapping("/refresh")
  fun refreshToken(
      request: HttpServletRequest,
      response: HttpServletResponse,
  ): ResponseEntity<TokenDto> {
    val refreshToken = getRefreshToken(request)
    val token = oauth2Service.refreshToken(refreshToken)
    setRefreshTokenCookie(token, response)
    return ResponseEntity.ok(token)
  }

  @PostMapping("/logout")
  fun logout(response: HttpServletResponse): ResponseEntity<Void> {
    clearRefreshTokenCookie(response)
    return ResponseEntity.noContent().build()
  }

  private fun setRefreshTokenCookie(token: TokenDto, response: HttpServletResponse) {
    if (token.refreshToken != null) {
      val refreshCookie = Cookie(REFRESH_TOKEN_COOKIE, token.refreshToken)
      refreshCookie.isHttpOnly = true
      refreshCookie.secure = authProperties.cookieSecure
      refreshCookie.path = "/auth/refresh"
      refreshCookie.maxAge = authProperties.refreshCookieMaxAge.toSeconds().toInt()
      refreshCookie.setAttribute("SameSite", "Lax")
      response.addCookie(refreshCookie)
    }
  }

  private fun clearRefreshTokenCookie(response: HttpServletResponse) {
    val cookie = Cookie(REFRESH_TOKEN_COOKIE, "")
    cookie.isHttpOnly = true
    cookie.secure = authProperties.cookieSecure
    cookie.path = "/auth/refresh"
    cookie.maxAge = 0
    cookie.setAttribute("SameSite", "Lax")
    response.addCookie(cookie)
  }

  private fun getRefreshToken(request: HttpServletRequest): String =
      request.cookies?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE }?.value
          ?: throw ProblemException(Problem.of(HttpStatus.UNAUTHORIZED.value()))
}
