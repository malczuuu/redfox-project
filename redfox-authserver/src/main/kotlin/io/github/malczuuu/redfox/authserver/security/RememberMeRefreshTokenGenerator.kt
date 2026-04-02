package io.github.malczuuu.redfox.authserver.security

import io.github.malczuuu.redfox.authserver.config.AuthServerProperties
import java.time.Clock
import java.time.Duration
import java.util.Base64
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator

internal class RememberMeRefreshTokenGenerator(
    private val properties: AuthServerProperties,
    private val clock: Clock,
) : OAuth2TokenGenerator<OAuth2RefreshToken> {

  private val keyGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96)

  override fun generate(context: OAuth2TokenContext): OAuth2RefreshToken? {
    if (context.tokenType != OAuth2TokenType.REFRESH_TOKEN) {
      return null
    }

    val rememberMe = shouldRememberLogin(context)
    val ttl = findLoginTtl(rememberMe, context)

    val issuedAt = clock.instant()
    val expiresAt = issuedAt.plus(ttl)
    return OAuth2RefreshToken(keyGenerator.generateKey(), issuedAt, expiresAt)
  }

  private fun shouldRememberLogin(context: OAuth2TokenContext): Boolean =
      context.authorization?.getAttribute<Boolean>(RememberMeAuthorizationService.REMEMBER_ME_ATTR)
          ?: false

  private fun findLoginTtl(rememberMe: Boolean, context: OAuth2TokenContext): Duration? =
      if (rememberMe) {
        properties.rememberMeRefreshTokenTtl
      } else {
        context.registeredClient.tokenSettings.refreshTokenTimeToLive
      }
}
