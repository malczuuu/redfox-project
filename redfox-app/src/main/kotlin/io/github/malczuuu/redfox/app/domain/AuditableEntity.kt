package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

// spotless:off
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AuditableEntity(

    id: UUID = UUID.randomUUID(),

    @Column(name = "project_version", nullable = false)
    var version: Long = 0,

    @CreatedDate
    @Column(name = "project_created_at", nullable = false)
    var createdAt: Instant? = null,

    @LastModifiedDate
    @Column(name = "project_updated_at", nullable = false)
    var updatedAt: Instant? = null

) : AbstractEntity(id)
// spotless:on
