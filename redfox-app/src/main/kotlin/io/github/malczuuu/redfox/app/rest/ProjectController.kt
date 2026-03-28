package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.core.CreateProjectDto
import io.github.malczuuu.redfox.app.core.ProjectDto
import io.github.malczuuu.redfox.app.core.ProjectService
import io.github.malczuuu.redfox.app.core.UpdateProjectDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

@Validated
@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(private val projectService: ProjectService) {

  @GetMapping
  fun getProjects(
      @RequestParam(defaultValue = "0") @PositiveOrZero page: Int,
      @RequestParam(defaultValue = "20") @Positive size: Int,
  ): PageResult<ProjectDto> = projectService.getProjects(PageRequest.of(page, size))

  @GetMapping("/{id}")
  fun getProject(@PathVariable id: UUID): ProjectDto = projectService.getProject(id)

  @PostMapping
  fun createProject(
      @RequestBody @Valid request: CreateProjectDto,
      uriBuilder: UriComponentsBuilder,
  ): ResponseEntity<Identity> {
    val identity = projectService.createProject(request)
    val location = uriBuilder.path("/api/v1/projects/{id}").buildAndExpand(identity.id).toUri()
    return ResponseEntity.created(location).body(identity)
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updateProject(@PathVariable id: UUID, @RequestBody @Valid request: UpdateProjectDto) =
      projectService.updateProject(id, request)

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteProject(@PathVariable id: UUID) = projectService.deleteProject(id)
}
