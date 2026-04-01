package io.github.malczuuu.redfox.authserver.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  fun authorizationServerFilterChain(http: HttpSecurity): SecurityFilterChain {
    http.oauth2AuthorizationServer { authorizationServer ->
      http.securityMatcher(authorizationServer.getEndpointsMatcher())
      authorizationServer.oidc(withDefaults())
    }
    http.authorizeHttpRequests { authorize -> authorize.anyRequest().authenticated() }
    http.oauth2ResourceServer { resourceServer -> resourceServer.jwt(withDefaults()) }
    http.exceptionHandling { exceptions ->
      val requestMatcher = MediaTypeRequestMatcher(MediaType.TEXT_HTML)
      requestMatcher.setIgnoredMediaTypes(setOf(MediaType.ALL))
      exceptions.defaultAuthenticationEntryPointFor(
          LoginUrlAuthenticationEntryPoint("/login"),
          requestMatcher,
      )
    }
    return http.build()
  }

  @Bean
  @Order(2)
  fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      authorizeHttpRequests {
        authorize("/actuator/**", permitAll)
        authorize(anyRequest, authenticated)
      }
      formLogin {}
    }
    return http.build()
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder {
    return BCryptPasswordEncoder()
  }
}
