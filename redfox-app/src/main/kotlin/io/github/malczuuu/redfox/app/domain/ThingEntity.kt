package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

// spotless:off
@Entity
@Table(name = "things")
@AttributeOverrides(
    AttributeOverride(name = "id", column = Column(name = "thing_id")),
    AttributeOverride(name = "version", column = Column(name = "thing_version")),
    AttributeOverride(name = "createdAt", column = Column(name = "thing_created_at")),
    AttributeOverride(name = "updatedAt", column = Column(name = "thing_updated_at")),
)
class ThingEntity(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: ProjectEntity,

    @field:Column(name = "thing_code", nullable = false, length = 255, unique = true)
    var code: String,

    @field:Column(name = "thing_name", nullable = false, length = 255)
    var name: String,

    @field:Column(name = "thing_description", nullable = false, length = 2048)
    var description: String,

    @field:Column(name = "thing_deleted_at")
    var deletedAt: Instant? = null

) : AuditableEntity()
// spotless:on
