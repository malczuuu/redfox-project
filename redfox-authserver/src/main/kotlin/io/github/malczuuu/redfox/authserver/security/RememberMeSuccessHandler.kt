package io.github.malczuuu.redfox.authserver.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler

class RememberMeSuccessHandler : AuthenticationSuccessHandler {

  private val delegate = SavedRequestAwareAuthenticationSuccessHandler()

  override fun onAuthenticationSuccess(
      request: HttpServletRequest,
      response: HttpServletResponse,
      authentication: Authentication,
  ) {
    val rememberMe = request.getParameter("remember-me")
    if (rememberMe != null) {
      request.session.setAttribute(REMEMBER_ME_ATTR, true)
    }
    delegate.onAuthenticationSuccess(request, response, authentication)
  }

  companion object {
    const val REMEMBER_ME_ATTR = "redfox_remember_me"
  }
}
