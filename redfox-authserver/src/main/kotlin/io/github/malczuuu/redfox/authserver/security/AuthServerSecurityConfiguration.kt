package io.github.malczuuu.redfox.authserver.security

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.malczuuu.redfox.authserver.config.AuthServerProperties
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
class AuthServerSecurityConfiguration(private val authServerProperties: AuthServerProperties) {

  @Bean
  @Order(1)
  fun authorizationServerFilterChain(
      http: HttpSecurity,
      tokenGenerator: OAuth2TokenGenerator<*>,
  ): SecurityFilterChain {
    http.oauth2AuthorizationServer {
      http.securityMatcher(it.endpointsMatcher)
      it.oidc(withDefaults())
      it.tokenGenerator(tokenGenerator)
    }
    http.authorizeHttpRequests { it.anyRequest().authenticated() }
    http.oauth2ResourceServer { it.jwt(withDefaults()) }
    http.exceptionHandling {
      val requestMatcher = MediaTypeRequestMatcher(MediaType.TEXT_HTML)
      requestMatcher.setIgnoredMediaTypes(setOf(MediaType.ALL))
      it.defaultAuthenticationEntryPointFor(
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
  fun authorizationService(): OAuth2AuthorizationService =
      RememberMeAuthorizationService(InMemoryOAuth2AuthorizationService())

  @Bean
  fun tokenGenerator(jwkSource: JWKSource<SecurityContext>, clock: Clock): OAuth2TokenGenerator<*> {
    val jwtGenerator = JwtGenerator(NimbusJwtEncoder(jwkSource))
    return DelegatingOAuth2TokenGenerator(
        jwtGenerator,
        OAuth2AccessTokenGenerator(),
        RememberMeRefreshTokenGenerator(authServerProperties, clock),
    )
  }

  @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
