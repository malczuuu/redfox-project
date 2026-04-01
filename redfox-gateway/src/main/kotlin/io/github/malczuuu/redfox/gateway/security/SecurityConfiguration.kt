package io.github.malczuuu.redfox.gateway.security

import io.github.malczuuu.redfox.gateway.oauth2.OAuth2CookieFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    private val oauth2CookieFilter: OAuth2CookieFilter,
) {

  @Bean
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      csrf { disable() }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      authorizeHttpRequests {
        authorize("/oauth2/callback", permitAll)
        authorize("/logout", permitAll)
        authorize("/actuator/**", permitAll)
        authorize("/**", permitAll)
      }
      addFilterBefore<UsernamePasswordAuthenticationFilter>(oauth2CookieFilter)
    }
    return http.build()
  }
}
