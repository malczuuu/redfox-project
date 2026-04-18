package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.UUID

/** `//` comments keep Spotless/ktfmt from changing annotation formatting. */
@MappedSuperclass
abstract class AbstractEntity(
    //
    @Id
    @field:Column(name = "id", nullable = false) //
    val id: UUID = UUID.randomUUID()
)
