package io.github.malczuuu.redfox.app.core

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class ThingDto(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val version: Long? = null,
)

data class CreateThingDto(
    @field:NotBlank @field:Size(max = 255) val code: String,
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 2048) val description: String,
)

data class UpdateThingDto(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 2048) val description: String,
    @field:NotNull val version: Long,
)
