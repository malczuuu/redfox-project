package io.github.malczuuu.redfox.authserver.security

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.jackson.SecurityJacksonModules
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.token.*
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableWebSecurity
@EnableJdbcHttpSession
@EnableConfigurationProperties(AuthServerSecurityProperties::class)
class AuthServerSecurityConfiguration {

  @Bean
  @Order(1000)
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
  @Order(2000)
  fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      authorizeHttpRequests {
        authorize("/.well-known/**", permitAll)
        authorize("/api/logout", permitAll)
        authorize("/actuator/**", permitAll)
        authorize(anyRequest, authenticated)
      }
      formLogin {}
      logout {
        logoutSuccessUrl = "/"
        permitAll()
      }
    }
    return http.build()
  }

  @Bean
  fun authorizationService(
      jdbcOperations: JdbcOperations,
      registeredClientRepository: RegisteredClientRepository,
  ): OAuth2AuthorizationService {
    val jsonMapper = getSecurityJsonMapper()

    val authorizationService =
        JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository)
    authorizationService.setAuthorizationRowMapper(
        JsonMapperOAuth2AuthorizationRowMapper(registeredClientRepository, jsonMapper)
    )
    authorizationService.setAuthorizationParametersMapper(
        JsonMapperOAuth2AuthorizationParametersMapper(jsonMapper)
    )

    return authorizationService
  }

  @Bean
  fun authorizationConsentService(
      jdbcOperations: JdbcOperations,
      registeredClientRepository: RegisteredClientRepository,
  ): OAuth2AuthorizationConsentService =
      JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository)

  @Bean
  fun jwkSource(properties: AuthServerSecurityProperties): JWKSource<SecurityContext> {
    val resource = DefaultResourceLoader().getResource(properties.keyStore.location)
    val password = properties.keyStore.password.toCharArray()
    val keyStore = KeyStore.getInstance("PKCS12")
    resource.inputStream.use { keyStore.load(it, password) }
    val rsaKey =
        RSAKey.Builder(keyStore.getCertificate(properties.keyStore.alias).publicKey as RSAPublicKey)
            .privateKey(keyStore.getKey(properties.keyStore.alias, password) as RSAPrivateKey)
            .keyID(properties.keyStore.alias)
            .build()
    return ImmutableJWKSet(JWKSet(rsaKey))
  }

  @Bean
  fun tokenGenerator(jwkSource: JWKSource<SecurityContext>): OAuth2TokenGenerator<*> {
    val jwtGenerator = JwtGenerator(NimbusJwtEncoder(jwkSource))
    return DelegatingOAuth2TokenGenerator(
        jwtGenerator,
        OAuth2AccessTokenGenerator(),
        OAuth2RefreshTokenGenerator(),
    )
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder =
      PasswordEncoderFactories.createDelegatingPasswordEncoder()

  private fun getSecurityJsonMapper(): JsonMapper =
      JsonMapper.builder()
          .addModules(
              SecurityJacksonModules.getModules(
                  AuthServerSecurityConfiguration::class.java.classLoader
              )
          )
          .build()
}
