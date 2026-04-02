package io.github.malczuuu.redfox.app.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver

class CookieBearerTokenResolver : BearerTokenResolver {

  companion object {
    const val ACCESS_TOKEN_COOKIE = "redfox_access_token"
  }

  private val defaultResolver = DefaultBearerTokenResolver()

  override fun resolve(request: HttpServletRequest): String? {
    if (request.requestURI.startsWith("/api/oauth2/")) {
      return null
    }
    val refreshedToken = request.getAttribute(TokenRefreshFilter.REFRESHED_TOKEN_ATTR) as? String
    if (refreshedToken != null) {
      return refreshedToken
    }
    val headerToken = defaultResolver.resolve(request)
    if (headerToken != null) {
      return headerToken
    }
    return request.cookies?.firstOrNull { it.name == ACCESS_TOKEN_COOKIE }?.value
  }
}
