package io.github.malczuuu.redfox.authserver.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "authserver.security")
data class AuthServerSecurityProperties(val keyStore: KeyStore = KeyStore()) {

  data class KeyStore(
      val location: String = "classpath:keystore-dev.p12",
      val password: String = "redfox-dev",
      val alias: String = "redfox-key",
  )
}
