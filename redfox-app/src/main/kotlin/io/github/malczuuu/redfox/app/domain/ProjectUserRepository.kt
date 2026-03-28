package io.github.malczuuu.redfox.app.domain

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectUserRepository : JpaRepository<ProjectUserEntity, UUID> {}
