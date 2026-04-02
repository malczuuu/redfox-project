package io.github.malczuuu.redfox.authserver.security

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthServerProperties::class)
class SecurityConfiguration(private val authServerProperties: AuthServerProperties) {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  fun authorizationServerFilterChain(
      http: HttpSecurity,
      tokenGenerator: OAuth2TokenGenerator<*>,
  ): SecurityFilterChain {
    http.oauth2AuthorizationServer { authorizationServer ->
      http.securityMatcher(authorizationServer.getEndpointsMatcher())
      authorizationServer.oidc(withDefaults())
      authorizationServer.tokenGenerator(tokenGenerator)
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
        authorize("/.well-known/**", permitAll)
        authorize("/api/logout", permitAll)
        authorize("/actuator/**", permitAll)
        authorize(anyRequest, authenticated)
      }
      formLogin { authenticationSuccessHandler = RememberMeSuccessHandler() }
      logout {
        logoutSuccessUrl = "/login?logout"
        permitAll()
      }
      rememberMe {}
    }
    return http.build()
  }

  @Bean
  fun authorizationService(): OAuth2AuthorizationService {
    return RememberMeAuthorizationService(InMemoryOAuth2AuthorizationService())
  }

  @Bean
  fun tokenGenerator(jwkSource: JWKSource<SecurityContext>): OAuth2TokenGenerator<*> {
    val jwtGenerator = JwtGenerator(NimbusJwtEncoder(jwkSource))
    return DelegatingOAuth2TokenGenerator(
        jwtGenerator,
        OAuth2AccessTokenGenerator(),
        RememberMeRefreshTokenGenerator(authServerProperties),
    )
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder {
    return BCryptPasswordEncoder()
  }
}
