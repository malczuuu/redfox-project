package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.UUID

// spotless:off
@MappedSuperclass
abstract class AbstractEntity(

    @Id
    @Column(name = "project_id", nullable = false)
    val id: UUID = UUID.randomUUID()

)
// spotless:on
