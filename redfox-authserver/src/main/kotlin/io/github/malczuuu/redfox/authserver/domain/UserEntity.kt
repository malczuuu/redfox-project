package io.github.malczuuu.redfox.authserver.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

// spotless:off
@Entity
@Table(name = "users")
class UserEntity(

    @field:Id
    @field:Column(name = "user_id")
    var id: UUID,

    @field:Column(name = "user_login", nullable = false, length = 255, unique = true)
    var login: String,

    @field:Column(name = "user_passhash", nullable = false, length = 255)
    var passhash: String,
)
// spotless:on
