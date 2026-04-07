package io.github.malczuuu.redfox.app.security

import io.github.malczuuu.redfox.app.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {

  companion object {
    private val log = LoggerFactory.getLogger(UserDetailsServiceImpl::class.java)
  }

  override fun loadUserByUsername(username: String): UserDetails {
    val user =
        userRepository.findByLogin(username).orElseThrow {
          UsernameNotFoundException.fromUsername(username)
        }

    log.info("User {} found", user)

    return User(user.login, user.passhash, listOf(SimpleGrantedAuthority("ROLE_USER")))
  }
}
