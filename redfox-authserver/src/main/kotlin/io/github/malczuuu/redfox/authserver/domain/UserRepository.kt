package io.github.malczuuu.redfox.authserver.domain

import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, UUID> {

  fun findByLogin(login: String): Optional<UserEntity>
}
