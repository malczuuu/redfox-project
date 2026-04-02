package io.github.malczuuu.redfox.authserver.config

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "redfox.authserver")
data class AuthServerProperties(val rememberMeRefreshTokenTtl: Duration = Duration.ofDays(90))
