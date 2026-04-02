package io.github.malczuuu.redfox.authserver.security

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class RememberMeAuthorizationService(
    private val delegate: OAuth2AuthorizationService,
) : OAuth2AuthorizationService {

  override fun save(authorization: OAuth2Authorization) {
    val enriched = enrichWithRememberMe(authorization)
    delegate.save(enriched)
  }

  override fun remove(authorization: OAuth2Authorization) {
    delegate.remove(authorization)
  }

  override fun findById(id: String): OAuth2Authorization? {
    return delegate.findById(id)
  }

  override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
    return delegate.findByToken(token, tokenType)
  }

  private fun enrichWithRememberMe(authorization: OAuth2Authorization): OAuth2Authorization {
    if (authorization.getAttribute<Boolean>(REMEMBER_ME_ATTR) == true) {
      return authorization
    }
    val request =
        (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: return authorization
    val rememberMe =
        request.session?.getAttribute(RememberMeSuccessHandler.REMEMBER_ME_ATTR) as? Boolean
            ?: false
    if (!rememberMe) {
      return authorization
    }
    return OAuth2Authorization.from(authorization).attribute(REMEMBER_ME_ATTR, true).build()
  }

  companion object {
    const val REMEMBER_ME_ATTR = "remember_me"
  }
}
