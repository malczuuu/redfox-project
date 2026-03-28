package io.github.malczuuu.redfox.app.core

import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.domain.ProjectRepository
import io.github.malczuuu.redfox.app.domain.ThingEntity
import io.github.malczuuu.redfox.app.domain.ThingRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ThingService(
    private val projectRepository: ProjectRepository,
    private val thingRepository: ThingRepository,
    private val clock: Clock,
) {

  fun getThings(projectId: UUID, pageable: Pageable): PageResult<ThingDto> {
    requireExistingProject(projectId)
    val things = thingRepository.findAll(projectId, pageable)
    return PageResult(
        content = things.content.map { it.toDto() },
        page = things.number,
        size = things.size,
        totalElements = things.totalElements,
    )
  }

  fun getThing(projectId: UUID, id: UUID): ThingDto {
    requireExistingProject(projectId)
    val thing = thingRepository.findOne(projectId, id).orElseThrow { ThingNotFoundException() }
    return thing.toDto()
  }

  @Transactional
  fun createThing(projectId: UUID, request: CreateThingDto): Identity {
    val project = projectRepository.findOne(projectId).orElseThrow { ProjectNotFoundException() }
    if (thingRepository.existsByCode(projectId, request.code)) {
      throw ThingAlreadyExistsException()
    }
    var thing =
        ThingEntity(
            project = project,
            code = request.code,
            name = request.name,
            description = request.description,
        )
    thing = thingRepository.save(thing)
    return Identity(thing.id.toString())
  }

  @Transactional
  fun updateThing(projectId: UUID, id: UUID, request: UpdateThingDto) {
    requireExistingProject(projectId)
    val thing = thingRepository.lockOne(projectId, id).orElseThrow { ThingNotFoundException() }
    thing.name = request.name
    thing.description = request.description
    thingRepository.save(thing)
  }

  @Transactional
  fun deleteThing(projectId: UUID, id: UUID) {
    requireExistingProject(projectId)
    thingRepository.lockOne(projectId, id).ifPresent { thing ->
      thing.deletedAt = Instant.now(clock)
      thingRepository.save(thing)
    }
  }

  private fun requireExistingProject(projectId: UUID) {
    projectRepository.findOne(projectId).orElseThrow { ProjectNotFoundException() }
  }

  private fun ThingEntity.toDto() =
      ThingDto(
          id = id,
          code = code,
          name = name,
          description = description,
          createdAt = createdAt,
          updatedAt = updatedAt,
          version = version,
      )
}
