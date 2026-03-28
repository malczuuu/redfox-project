package io.github.malczuuu.redfox.app

import java.time.Clock
import java.time.ZoneId
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class Application {

  @Bean fun clock(): Clock = Clock.system(ZoneId.systemDefault())
}

fun main(args: Array<String>) {
  runApplication<Application>(*args)
}
