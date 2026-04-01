package io.github.malczuuu.redfox.gateway.config

import io.github.malczuuu.redfox.gateway.oauth2.OAuth2CookieFilter
import io.github.malczuuu.redfox.gateway.security.CookieCrypto
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableConfigurationProperties(GatewayProperties::class)
class GatewayConfiguration {

  @Bean
  fun cookieCrypto(properties: GatewayProperties): CookieCrypto {
    val keyBytes = Base64.getDecoder().decode(properties.cookie.secret)
    val secretKey = SecretKeySpec(keyBytes, "AES")
    return CookieCrypto(secretKey)
  }

  @Bean
  fun oauthRestClient(): RestClient {
    return RestClient.create()
  }

  @Bean
  fun oauth2CookieFilter(
      properties: GatewayProperties,
      cookieCrypto: CookieCrypto,
      restClient: RestClient,
      jsonMapper: JsonMapper,
  ): OAuth2CookieFilter {
    return OAuth2CookieFilter(properties, cookieCrypto, restClient, jsonMapper)
  }
}
