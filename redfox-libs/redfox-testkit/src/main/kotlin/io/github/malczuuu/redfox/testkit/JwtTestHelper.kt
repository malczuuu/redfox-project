package io.github.malczuuu.redfox.testkit

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date

object JwtTestHelper {

  val RSA_KEY: RSAKey = generateRsaKey()

  fun generateToken(subject: String): String {
    val now = Instant.now()
    val claims =
        JWTClaimsSet.Builder()
            .subject(subject)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(3600)))
            .build()
    val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims)
    jwt.sign(RSASSASigner(RSA_KEY))
    return jwt.serialize()
  }

  private fun generateRsaKey(): RSAKey {
    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    return RSAKey.Builder(keyPair.public as RSAPublicKey)
        .privateKey(keyPair.private as RSAPrivateKey)
        .keyID("test-key")
        .build()
  }
}
