package io.github.malczuuu.redfox.app.core

import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.domain.ProjectEntity
import io.github.malczuuu.redfox.app.domain.ProjectRepository
import io.github.malczuuu.redfox.app.domain.ThingRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val thingRepository: ThingRepository,
    private val clock: Clock,
) {

  fun getProjects(pageable: Pageable): PageResult<ProjectDto> {
    val projects = projectRepository.findAll(pageable)
    return PageResult(
        content = projects.content.map { it.toDto() },
        page = projects.number,
        size = projects.size,
        totalElements = projects.totalElements,
    )
  }

  fun getProject(id: UUID): ProjectDto {
    val project = projectRepository.findOne(id).orElseThrow { ProjectNotFoundException() }
    return project.toDto()
  }

  @Transactional
  fun createProject(request: CreateProjectDto): Identity {
    if (projectRepository.existsByCode(request.code)) {
      throw ProjectAlreadyExistsException()
    }
    var project =
        ProjectEntity(code = request.code, name = request.name, description = request.description)
    project = projectRepository.save(project)
    return Identity(project.id.toString())
  }

  @Transactional
  fun updateProject(id: UUID, request: UpdateProjectDto) {
    val project = projectRepository.lockOne(id).orElseThrow { ProjectNotFoundException() }
    project.name = request.name
    project.description = request.description
    projectRepository.save(project)
  }

  @Transactional
  fun deleteProject(id: UUID) {
    projectRepository.lockOne(id).ifPresent { project ->
      val now = Instant.now(clock)
      project.deletedAt = now
      projectRepository.save(project)

      val things = thingRepository.findAll(id, Pageable.unpaged())
      things.content.forEach { thing ->
        thing.deletedAt = now
        thingRepository.save(thing)
      }
    }
  }

  private fun ProjectEntity.toDto() =
      ProjectDto(
          id = id,
          code = code,
          name = name,
          description = description,
          createdAt = createdAt,
          updatedAt = updatedAt,
          version = version,
      )
}
