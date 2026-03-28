package io.github.malczuuu.redfox.app.core

import io.github.malczuuu.redfox.app.common.Identity
import io.github.malczuuu.redfox.app.common.PageResult
import io.github.malczuuu.redfox.app.domain.UserEntity
import io.github.malczuuu.redfox.app.domain.UserRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {

  fun getUsers(pageable: Pageable): PageResult<UserDto> {
    val users = userRepository.findAll(pageable)
    return PageResult(
        content = users.content.map { it.toDto() },
        page = users.number,
        size = users.size,
        totalElements = users.totalElements,
    )
  }

  fun getUser(id: UUID): UserDto {
    val user = userRepository.findOne(id).orElseThrow { UserNotFoundException() }
    return user.toDto()
  }

  @Transactional
  fun createUser(request: CreateUserDto): Identity {
    if (userRepository.existsByLogin(request.login)) {
      throw UserAlreadyExistsException()
    }
    var user =
        UserEntity(
            login = request.login,
            passhash = passwordEncoder.encode(request.passhash)!!,
            firstName = request.firstName,
            lastName = request.lastName,
        )
    user = userRepository.save(user)
    return Identity(user.id.toString())
  }

  @Transactional
  fun updateUser(id: UUID, request: UpdateUserDto) {
    val user = userRepository.lockOne(id).orElseThrow { UserNotFoundException() }
    user.firstName = request.firstName
    user.lastName = request.lastName
    userRepository.save(user)
  }

  @Transactional
  fun deleteUser(id: UUID) {
    userRepository.lockOne(id).ifPresent { user ->
      user.deletedAt = Instant.now(clock)
      userRepository.save(user)
    }
  }

  private fun UserEntity.toDto() =
      UserDto(
          id = id,
          login = login,
          firstName = firstName,
          lastName = lastName,
          createdAt = createdAt,
          updatedAt = updatedAt,
          version = version,
      )
}
