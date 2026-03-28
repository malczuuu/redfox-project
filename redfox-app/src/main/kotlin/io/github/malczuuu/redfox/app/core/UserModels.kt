package io.github.malczuuu.redfox.app.core

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val login: String,
    val firstName: String,
    val lastName: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val version: Long? = null,
)

data class CreateUserDto(
    @field:NotBlank @field:Size(max = 255) val login: String,
    @field:NotBlank @field:Size(max = 255) val passhash: String,
    @field:NotBlank @field:Size(max = 255) val firstName: String,
    @field:NotBlank @field:Size(max = 255) val lastName: String,
)

data class UpdateUserDto(
    @field:NotBlank @field:Size(max = 255) val firstName: String,
    @field:NotBlank @field:Size(max = 255) val lastName: String,
    @field:NotNull val version: Long,
)
