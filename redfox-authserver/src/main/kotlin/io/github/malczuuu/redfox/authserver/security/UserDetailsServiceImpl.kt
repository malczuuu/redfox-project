package io.github.malczuuu.redfox.authserver.security

import io.github.malczuuu.redfox.authserver.domain.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {

  override fun loadUserByUsername(username: String): UserDetails {
    val user =
        userRepository.findByLogin(username).orElseThrow {
          UsernameNotFoundException.fromUsername(username)
        }

    return User(user.login, user.passhash, listOf(SimpleGrantedAuthority("ROLE_USER")))
  }
}
