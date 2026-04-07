package io.github.malczuuu.redfox.app.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "redfox.auth")
data class AuthProperties(
    val authserverTokenUri: String,
    val clientId: String,
    val clientSecret: String,
    val cookieDomain: String,
    val cookieSecure: Boolean,
    val passwordEncoder: String = "bcrypt",
    val basic: Basic = Basic(),
) {

  data class Basic(val enabled: Boolean = false)
}
