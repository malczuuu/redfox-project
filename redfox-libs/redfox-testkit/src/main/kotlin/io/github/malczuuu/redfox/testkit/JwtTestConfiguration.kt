package io.github.malczuuu.redfox.testkit

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@TestConfiguration
class JwtTestConfiguration {

  @Bean
  fun jwtDecoder(): JwtDecoder =
      NimbusJwtDecoder.withPublicKey(JwtTestHelper.RSA_KEY.toRSAPublicKey()).build()
}
