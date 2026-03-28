package io.github.malczuuu.redfox.flyway

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@ContainerTest
@SpringBootTest(classes = [FlywayApplication::class])
class FlywayApplicationTests : PostgresAwareTest {

  @Test fun contextLoads() {}
}
