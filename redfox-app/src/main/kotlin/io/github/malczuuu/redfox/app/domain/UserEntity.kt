package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
@AttributeOverrides(
    AttributeOverride(name = "id", column = Column(name = "user_id")),
    AttributeOverride(name = "version", column = Column(name = "user_version")),
    AttributeOverride(name = "createdAt", column = Column(name = "user_created_at")),
    AttributeOverride(name = "updatedAt", column = Column(name = "user_updated_at")),
)
class UserEntity(
    //
    @field:Column(name = "user_login", nullable = false, length = 255, unique = true) //
    var login: String,
    //
    @field:Column(name = "user_passhash", nullable = false, length = 255) //
    var passhash: String,
    //
    @field:Column(name = "user_first_name", nullable = false, length = 255) //
    var firstName: String,
    //
    @field:Column(name = "user_last_name", nullable = false, length = 255) //
    var lastName: String,
    //
    @field:Column(name = "user_deleted_at") //
    var deletedAt: Instant? = null,
) : AuditableEntity()
