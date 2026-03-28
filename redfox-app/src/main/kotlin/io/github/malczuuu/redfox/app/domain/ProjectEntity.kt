package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

// spotless:off
@Entity
@Table(name = "projects")
@AttributeOverrides(
    AttributeOverride(name = "id", column = Column(name = "project_id")),
    AttributeOverride(name = "version", column = Column(name = "project_version")),
    AttributeOverride(name = "createdAt", column = Column(name = "project_created_at")),
    AttributeOverride(name = "updatedAt", column = Column(name = "project_updated_at")),
)
class ProjectEntity(

    @field:Column(name = "project_code", nullable = false, length = 255, unique = true)
    var code: String,

    @field:Column(name = "project_name", nullable = false, length = 255)
    var name: String,

    @field:Column(name = "project_description", nullable = false, length = 2048)
    var description: String,

    @field:Column(name = "project_deleted_at")
    var deletedAt: Instant? = null

) : AuditableEntity()
// spotless:on
