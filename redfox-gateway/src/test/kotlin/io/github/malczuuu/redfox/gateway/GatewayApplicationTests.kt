package io.github.malczuuu.redfox.gateway

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(profiles = ["test"])
@SpringBootTest(classes = [GatewayApplication::class])
class GatewayApplicationTests {

  @Test fun contextLoads() {}
}
