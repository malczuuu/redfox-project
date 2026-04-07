package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.LockModeType
import java.util.Optional
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<UserEntity, UUID> {

  @Query("SELECT u FROM UserEntity u WHERE u.deletedAt IS NULL ORDER BY u.login ASC")
  override fun findAll(pageable: Pageable): Page<UserEntity>

  @Query("SELECT u FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
  fun findOne(id: UUID): Optional<UserEntity>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
  fun lockOne(id: UUID): Optional<UserEntity>

  @Query(
      """
      SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
      FROM
          UserEntity u
      WHERE
          u.login = :login
          AND u.deletedAt IS NULL
      """
  )
  fun existsByLogin(login: String): Boolean

  @Query("SELECT u FROM UserEntity u WHERE u.login = :username AND u.deletedAt IS NULL")
  fun findByLogin(username: String): Optional<UserEntity>
}
