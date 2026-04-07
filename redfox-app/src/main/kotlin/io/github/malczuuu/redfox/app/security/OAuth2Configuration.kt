package io.github.malczuuu.redfox.app.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class OAuth2Configuration {

  @Bean fun authServerRestClient(): RestClient = RestClient.create()
}
