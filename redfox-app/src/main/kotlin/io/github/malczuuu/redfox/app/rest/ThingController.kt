package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.core.CreateThingDto
import io.github.malczuuu.redfox.app.core.ThingDto
import io.github.malczuuu.redfox.app.core.ThingService
import io.github.malczuuu.redfox.app.core.UpdateThingDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
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
@RequestMapping("/api/v1/projects/{projectId}/things")
class ThingController(private val thingService: ThingService) {

  @GetMapping(produces = [APPLICATION_JSON_VALUE])
  fun getThings(
      @PathVariable projectId: UUID,
      @RequestParam(defaultValue = "0") @PositiveOrZero page: Int,
      @RequestParam(defaultValue = "20") @Positive size: Int,
  ): PageResult<ThingDto> = thingService.getThings(projectId, PageRequest.of(page, size))

  @GetMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
  fun getThing(@PathVariable projectId: UUID, @PathVariable id: UUID): ThingDto =
      thingService.getThing(projectId, id)

  @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
  fun createThing(
      @PathVariable projectId: UUID,
      @RequestBody @Valid request: CreateThingDto,
      uriBuilder: UriComponentsBuilder,
  ): ResponseEntity<Identity> {
    val identity = thingService.createThing(projectId, request)
    val location =
        uriBuilder
            .path("/api/v1/projects/{projectId}/things/{id}")
            .buildAndExpand(projectId, identity.id)
            .toUri()
    return ResponseEntity.created(location).body(identity)
  }

  @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updateThing(
      @PathVariable projectId: UUID,
      @PathVariable id: UUID,
      @RequestBody @Valid request: UpdateThingDto,
  ) = thingService.updateThing(projectId, id, request)

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteThing(@PathVariable projectId: UUID, @PathVariable id: UUID) =
      thingService.deleteThing(projectId, id)
}
