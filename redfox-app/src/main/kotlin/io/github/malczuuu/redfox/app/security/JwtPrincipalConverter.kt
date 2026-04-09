package io.github.malczuuu.redfox.app.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component

@Component
class JwtPrincipalConverter(private val userDetailsService: UserDetailsService) :
    Converter<Jwt, AbstractAuthenticationToken> {

  override fun convert(source: Jwt): AbstractAuthenticationToken {
    val user = userDetailsService.loadUserByUsername(source.subject)
    return PreAuthenticatedAuthenticationToken(user, source.tokenValue, user.authorities)
  }
}
