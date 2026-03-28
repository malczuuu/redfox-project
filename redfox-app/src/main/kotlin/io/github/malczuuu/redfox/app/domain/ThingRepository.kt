package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.LockModeType
import java.util.Optional
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ThingRepository : JpaRepository<ThingEntity, UUID> {

  @Query(
      """
      SELECT t
      FROM
          ThingEntity t
      WHERE
          t.project.id = :projectId
          AND t.deletedAt IS NULL
      ORDER BY
          t.code ASC
      """
  )
  fun findAll(projectId: UUID, pageable: Pageable): Page<ThingEntity>

  @Query(
      """
      SELECT t
      FROM
          ThingEntity t
      WHERE
          t.project.id = :projectId
          AND t.id = :id
          AND t.deletedAt IS NULL
      """
  )
  fun findOne(projectId: UUID, id: UUID): Optional<ThingEntity>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT t
      FROM
          ThingEntity t
      WHERE
          t.project.id = :projectId
          AND t.id = :id
          AND t.deletedAt IS NULL
      """
  )
  fun lockOne(projectId: UUID, id: UUID): Optional<ThingEntity>

  @Query(
      """
      SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
      FROM
        ThingEntity t
      WHERE
          t.project.id = :projectId
          AND t.code = :code
          AND t.deletedAt IS NULL
      """
  )
  fun existsByCode(projectId: UUID, code: String): Boolean
}
