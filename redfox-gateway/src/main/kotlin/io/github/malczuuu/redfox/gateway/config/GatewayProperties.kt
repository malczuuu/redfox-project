package io.github.malczuuu.redfox.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "redfox.gateway")
data class GatewayProperties(
    val cookie: CookieProperties = CookieProperties(),
    val oauth2: OAuth2Properties = OAuth2Properties(),
) {

  data class CookieProperties(
      val secret: String = "",
  )

  data class OAuth2Properties(
      val clientId: String = "redfox-gateway",
      val clientSecret: String = "redfox-gateway-secret",
      val authserverUrl: String = "http://localhost:8483",
      val redirectUri: String = "http://localhost:8480/oauth2/callback",
  )
}
