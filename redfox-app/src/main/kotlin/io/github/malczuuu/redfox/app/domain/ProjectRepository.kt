package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.LockModeType
import java.util.Optional
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ProjectRepository : JpaRepository<ProjectEntity, UUID> {

  @Query("SELECT p FROM ProjectEntity p WHERE p.deletedAt IS NULL ORDER BY p.code ASC")
  override fun findAll(pageable: Pageable): Page<ProjectEntity>

  @Query("SELECT p FROM ProjectEntity p WHERE p.id = :id AND p.deletedAt IS NULL")
  fun findOne(id: UUID): Optional<ProjectEntity>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM ProjectEntity p WHERE p.id = :id AND p.deletedAt IS NULL")
  fun lockOne(id: UUID): Optional<ProjectEntity>

  @Query(
      """
      SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
      FROM
          ProjectEntity p
      WHERE
          p.code = :code
          AND p.deletedAt IS NULL
      """
  )
  fun existsByCode(code: String): Boolean
}
