package io.github.malczuuu.redfox.authserver

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.authserver.AuthServerApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(profiles = ["test"])
@ContainerTest
@SpringBootTest(classes = [AuthServerApplication::class])
class AuthServerApplicationTests : PostgresAwareTest {

  @Test fun contextLoads() {}
}
