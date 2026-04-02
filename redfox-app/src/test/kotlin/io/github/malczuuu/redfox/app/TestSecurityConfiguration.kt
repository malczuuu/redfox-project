package io.github.malczuuu.redfox.app

import java.time.Instant
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Profile("test")
@Configuration
class TestSecurityConfiguration {

  @Bean
  fun jwtDecoder(): JwtDecoder = JwtDecoder {
    Jwt.withTokenValue("mock-token")
        .header("alg", "none")
        .claim("sub", "test-user")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build()
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      csrf { disable() }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      authorizeHttpRequests { authorize(anyRequest, permitAll) }
    }
    return http.build()
  }
}
