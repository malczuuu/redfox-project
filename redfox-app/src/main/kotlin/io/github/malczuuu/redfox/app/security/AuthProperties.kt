package io.github.malczuuu.redfox.app.security

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "redfox.auth")
data class AuthProperties(
    val authserverTokenUri: String,
    val clientId: String,
    val clientSecret: String,
    val cookieDomain: String,
    val cookieSecure: Boolean,
    val refreshCookieMaxAge: Duration,
    val basic: Basic = Basic(),
) {

  data class Basic(val enabled: Boolean = false)
}
