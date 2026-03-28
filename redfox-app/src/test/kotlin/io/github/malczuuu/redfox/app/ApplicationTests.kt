package io.github.malczuuu.redfox.app

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(profiles = ["test"])
@ContainerTest
@SpringBootTest(classes = [Application::class])
class ApplicationTests : PostgresAwareTest {

  @Test fun contextLoads() {}
}
