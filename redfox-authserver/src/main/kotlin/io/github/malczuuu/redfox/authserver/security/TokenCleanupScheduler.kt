package io.github.malczuuu.redfox.authserver.security

import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TokenCleanupScheduler(
    private val jdbcTemplate: JdbcTemplate,
    private val clock: Clock,
) {

  companion object {
    private val log = LoggerFactory.getLogger(TokenCleanupScheduler::class.java)
  }

  @Scheduled(cron = "0 * * * * *")
  @Transactional
  fun cleanupExpiredTokens() {
    val now = clock.instant()

    val sql =
        """
        DELETE FROM oauth2_authorization 
        WHERE access_token_expires_at < ? 
          AND (refresh_token_expires_at IS NULL OR refresh_token_expires_at < ?)
          AND (authorization_code_expires_at IS NULL OR authorization_code_expires_at < ?)
        """
            .trimIndent()

    val rowsDeleted = jdbcTemplate.update(sql, now, now, now)

    log.debug("Cleaned up count={} expired tokens from oauth2_authorization table", rowsDeleted)
  }
}
