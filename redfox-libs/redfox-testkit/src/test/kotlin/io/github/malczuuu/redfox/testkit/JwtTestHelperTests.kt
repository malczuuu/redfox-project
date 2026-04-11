package io.github.malczuuu.redfox.testkit

import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JwtTestHelperTests {

  @Test
  fun `should generate valid signed JWT`() {
    val token = JwtTestHelper.generateToken("user123")

    val jwt = SignedJWT.parse(token)

    val verifier: JWSVerifier = RSASSAVerifier(JwtTestHelper.RSA_KEY.toRSAPublicKey())

    assertThat(jwt.verify(verifier)).isTrue()
  }

  @Test
  fun `should contain correct subject`() {
    val token = JwtTestHelper.generateToken("user123")

    val jwt = SignedJWT.parse(token)

    assertThat(jwt.jwtClaimsSet.subject).isEqualTo("user123")
  }

  @Test
  fun `should contain issued at and expiration`() {
    val before = Instant.now()

    val token = JwtTestHelper.generateToken("user123")

    val after = Instant.now()

    val jwt = SignedJWT.parse(token)
    val claims = jwt.jwtClaimsSet

    assertThat(claims.issueTime.toInstant()).isBetween(before.minusSeconds(1), after.plusSeconds(1))

    assertThat(claims.expirationTime.toInstant())
        .isBetween(before.plusSeconds(3599), after.plusSeconds(3601))
  }

  @Test
  fun `should expire after one hour`() {
    val token = JwtTestHelper.generateToken("user123")

    val jwt = SignedJWT.parse(token)
    val exp = jwt.jwtClaimsSet.expirationTime.toInstant()
    val iat = jwt.jwtClaimsSet.issueTime.toInstant()

    assertThat(exp.epochSecond - iat.epochSecond).isEqualTo(3600)
  }

  @Test
  fun `should use RS256 algorithm`() {
    val token = JwtTestHelper.generateToken("user123")

    val jwt = SignedJWT.parse(token)

    assertThat(jwt.header.algorithm.name).isEqualTo("RS256")
  }

  @Test
  fun `should include key id`() {
    val token = JwtTestHelper.generateToken("user123")

    val jwt = SignedJWT.parse(token)

    assertThat(jwt.header.keyID).isEqualTo("test-key")
  }
}
