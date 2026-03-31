package io.github.malczuuu.redfox.authserver.core

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    private val username: String,
    private val password: String?,
    private val authorities: Collection<GrantedAuthority>,
) : UserDetails {

  override fun getUsername(): String = username

  override fun getPassword(): String? = password

  override fun getAuthorities(): Collection<GrantedAuthority> = authorities
}
