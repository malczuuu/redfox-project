package io.github.malczuuu.redfox.app.security

import io.github.problem4j.core.Problem
import io.github.problem4j.core.ProblemBuilder
import io.github.problem4j.core.ProblemContext
import io.github.problem4j.spring.webmvc.resolver.NoHandlerFoundProblemResolver
import io.github.problem4j.spring.webmvc.resolver.NoResourceFoundProblemResolver
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties::class)
class SecurityConfiguration(jsonMapper: JsonMapper) {

  private val problemString401 =
      jsonMapper.writeValueAsString(Problem.of(HttpStatus.UNAUTHORIZED.value()))

  @Bean
  fun securityFilterChain(
      http: HttpSecurity,
      authProperties: AuthProperties,
      userDetailsService: UserDetailsService,
  ): SecurityFilterChain {
    http {
      csrf { disable() }

      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      authorizeHttpRequests {
        authorize("/api/oauth2/**", permitAll)
        authorize("/api/v1/**", authenticated)
        authorize("/actuator/**", permitAll)
        authorize("/swagger-ui/**", permitAll)
        authorize("/v3/api-docs", permitAll)
        authorize("/v3/api-docs/swagger-config", permitAll)
        authorize("/**", denyAll)
      }
      oauth2ResourceServer {
        jwt {
          jwtAuthenticationConverter = {
            val login = it.subject
            val user = userDetailsService.loadUserByUsername(login)
            PreAuthenticatedAuthenticationToken(user, it.tokenValue, user.authorities)
          }
        }
        bearerTokenResolver = CookieBearerTokenResolver()
      }

      if (authProperties.basic.enabled) {
        httpBasic {}
      }

      exceptionHandling {
        authenticationEntryPoint = { _, response, _ -> write401(response) }
        accessDeniedHandler = { _, response, _ -> write401(response) }
      }
    }
    return http.build()
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder =
      PasswordEncoderFactories.createDelegatingPasswordEncoder()

  @Bean
  fun noHandlerFoundProblemResolver(): NoHandlerFoundProblemResolver =
      ExtNoHandlerFoundProblemResolver()

  @Bean
  fun noResourceFoundProblemResolver(): NoResourceFoundProblemResolver =
      ExtNoResourceFoundProblemResolver()

  private fun write401(response: HttpServletResponse) {
    response.status = HttpStatus.UNAUTHORIZED.value()
    response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
    response.writer.write(problemString401)
  }

  private open class ExtNoHandlerFoundProblemResolver : NoHandlerFoundProblemResolver() {
    override fun resolveBuilder(
        context: ProblemContext,
        ex: Exception,
        headers: HttpHeaders,
        status: HttpStatusCode,
    ): ProblemBuilder = Problem.builder().status(HttpStatus.UNAUTHORIZED.value())
  }

  private open class ExtNoResourceFoundProblemResolver : NoResourceFoundProblemResolver() {
    override fun resolveBuilder(
        context: ProblemContext,
        ex: Exception,
        headers: HttpHeaders,
        status: HttpStatusCode,
    ): ProblemBuilder = Problem.builder().status(HttpStatus.UNAUTHORIZED.value())
  }
}
