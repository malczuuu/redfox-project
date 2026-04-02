package io.github.malczuuu.redfox.authserver

import io.github.malczuuu.redfox.authserver.config.AuthServerProperties
import java.time.Clock
import java.time.ZoneId
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableConfigurationProperties(AuthServerProperties::class)
class AuthServerApplication {

  @Bean fun clock(): Clock = Clock.system(ZoneId.systemDefault())
}

fun main(args: Array<String>) {
  runApplication<AuthServerApplication>(*args)
}
