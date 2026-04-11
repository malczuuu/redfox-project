package io.github.malczuuu.redfox.authserver.domain

import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<UserEntity, UUID> {

  @Query("SELECT u FROM UserEntity u WHERE u.login = :login AND u.deletedAt IS NULL")
  fun findByLogin(login: String): Optional<UserEntity>
}
