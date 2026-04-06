package io.github.malczuuu.redfox.authserver

import java.time.Clock
import java.time.ZoneId
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class AuthServerApplication {

  @Bean fun clock(): Clock = Clock.system(ZoneId.systemDefault())
}

fun main(args: Array<String>) {
  runApplication<AuthServerApplication>(*args)
}
