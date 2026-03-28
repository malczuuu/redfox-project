package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.core.CreateUserDto
import io.github.malczuuu.redfox.app.core.UpdateUserDto
import io.github.malczuuu.redfox.app.core.UserDto
import io.github.malczuuu.redfox.app.core.UserService
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
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

  @GetMapping
  fun getUsers(
      @RequestParam(defaultValue = "0") @PositiveOrZero page: Int,
      @RequestParam(defaultValue = "20") @Positive size: Int,
  ): PageResult<UserDto> = userService.getUsers(PageRequest.of(page, size))

  @GetMapping("/{id}") fun getUser(@PathVariable id: UUID): UserDto = userService.getUser(id)

  @PostMapping
  fun createUser(
      @RequestBody @Valid request: CreateUserDto,
      uriBuilder: UriComponentsBuilder,
  ): ResponseEntity<Identity> {
    val identity = userService.createUser(request)
    val location = uriBuilder.path("/api/v1/users/{id}").buildAndExpand(identity.id).toUri()
    return ResponseEntity.created(location).body(identity)
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updateUser(@PathVariable id: UUID, @RequestBody @Valid request: UpdateUserDto) {
    userService.updateUser(id, request)
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteUser(@PathVariable id: UUID) {
    userService.deleteUser(id)
  }
}
