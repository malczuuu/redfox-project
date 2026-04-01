package io.github.malczuuu.redfox.gateway.oauth2

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LogoutController {

  @GetMapping("/logout")
  fun logout(response: HttpServletResponse) {
    val accessCookie = Cookie("gw_access", "")
    accessCookie.path = "/"
    accessCookie.maxAge = 0
    accessCookie.isHttpOnly = true
    response.addCookie(accessCookie)

    val contextCookie = Cookie("gw_context", "")
    contextCookie.path = "/"
    contextCookie.maxAge = 0
    contextCookie.isHttpOnly = true
    response.addCookie(contextCookie)

    response.sendRedirect("/")
  }
}
