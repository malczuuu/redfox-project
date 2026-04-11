package io.github.malczuuu.redfox.authserver.security

import io.github.malczuuu.checkmate.annotation.ContainerTest
import io.github.malczuuu.checkmate.container.PostgresAwareTest
import io.github.malczuuu.redfox.authserver.AuthServerApplication
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(profiles = ["test"])
@ContainerTest
@SpringBootTest(classes = [AuthServerApplication::class])
class UserDetailsServiceImplTests : PostgresAwareTest {

  @Autowired private lateinit var userDetailsService: UserDetailsService
  @Autowired private lateinit var passwordEncoder: PasswordEncoder
  @Autowired private lateinit var jdbcTemplate: JdbcTemplate

  @BeforeEach
  fun beforeEach() {
    jdbcTemplate.execute("DELETE FROM users")
  }

  @Test
  fun givenExistingUser_whenLoadUserByUsername_thenReturnsUserDetails() {
    jdbcTemplate.update(
        "INSERT INTO users (user_id, user_login, user_passhash, user_first_name, user_last_name) VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        "admin",
        passwordEncoder.encode("secret"),
        "Admin",
        "Admin",
    )

    val userDetails = userDetailsService.loadUserByUsername("admin")

    assertThat(userDetails.username).isEqualTo("admin")
    assertThat(userDetails.authorities.map { it.authority }).containsExactly("ROLE_USER")
  }

  @Test
  fun givenUnknownUser_whenLoadUserByUsername_thenThrowsUsernameNotFoundException() {
    assertThatThrownBy { userDetailsService.loadUserByUsername("unknown") }
        .isInstanceOf(UsernameNotFoundException::class.java)
  }
}
