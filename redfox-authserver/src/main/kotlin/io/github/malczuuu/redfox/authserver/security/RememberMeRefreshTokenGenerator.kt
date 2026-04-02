package io.github.malczuuu.redfox.authserver.security

import java.time.Instant
import java.util.Base64
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator

class RememberMeRefreshTokenGenerator(private val properties: AuthServerProperties) :
    OAuth2TokenGenerator<OAuth2RefreshToken> {

  private val keyGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96)

  override fun generate(context: OAuth2TokenContext): OAuth2RefreshToken? {
    if (context.tokenType != OAuth2TokenType.REFRESH_TOKEN) {
      return null
    }
    val issuedAt = Instant.now()
    val rememberMe =
        context.authorization?.getAttribute<Boolean>(
            RememberMeAuthorizationService.REMEMBER_ME_ATTR
        ) ?: false
    val ttl =
        if (rememberMe) {
          properties.rememberMeRefreshTokenTtl
        } else {
          context.registeredClient.tokenSettings.refreshTokenTimeToLive
        }
    val expiresAt = issuedAt.plus(ttl)
    return OAuth2RefreshToken(keyGenerator.generateKey(), issuedAt, expiresAt)
  }
}
