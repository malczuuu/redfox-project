package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.UUID

@MappedSuperclass
abstract class AbstractEntity(
    //
    @Id
    @field:Column(name = "id", nullable = false) //
    val id: UUID = UUID.randomUUID()
)
