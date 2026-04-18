package io.github.malczuuu.redfox.app.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "project_users")
@AttributeOverrides(
    AttributeOverride(name = "id", column = Column(name = "project_user_id")),
)
class ProjectUserEntity(
    //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", updatable = false) //
    var project: ProjectEntity,
    //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false) //
    var user: UserEntity,
) : AbstractEntity()
