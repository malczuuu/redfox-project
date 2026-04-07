package io.github.malczuuu.redfox.app.security

import io.github.problem4j.core.Problem
import io.github.problem4j.core.ProblemException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.json.JsonMapper

@Service
class OAuth2Service(
    private val authProperties: AuthProperties,
    private val authServerRestClient: RestClient,
    private val jsonMapper: JsonMapper,
) {

  fun exchangeToken(code: String, redirectUri: String, codeVerifier: String): TokenDto {
    val formData = LinkedMultiValueMap<String, String>()
    formData.add("grant_type", "authorization_code")
    formData.add("code", code)
    formData.add("redirect_uri", redirectUri)
    formData.add("code_verifier", codeVerifier)
    return callTokenEndpoint(formData)
  }

  fun refreshToken(refreshToken: String): TokenDto {
    val formData = LinkedMultiValueMap<String, String>()
    formData.add("grant_type", "refresh_token")
    formData.add("refresh_token", refreshToken)
    return callTokenEndpoint(formData)
  }

  private fun callTokenEndpoint(formData: LinkedMultiValueMap<String, String>): TokenDto {
    try {
      val body =
          authServerRestClient
              .post()
              .uri(authProperties.authserverTokenUri)
              .headers { it.setBasicAuth(authProperties.clientId, authProperties.clientSecret) }
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(formData)
              .retrieve()
              .body(String::class.java)
              ?: throw ProblemException(Problem.of(HttpStatus.UNAUTHORIZED.value()))
      val json = jsonMapper.readTree(body)
      return TokenDto(
          accessToken = json.get("access_token").stringValue(),
          refreshToken = json.get("refresh_token")?.stringValue(),
          expiresIn = json.get("expires_in")?.asInt() ?: 1800,
      )
    } catch (_: RestClientResponseException) {
      throw ProblemException(Problem.of(HttpStatus.UNAUTHORIZED.value()))
    }
  }
}
