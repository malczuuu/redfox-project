package io.github.malczuuu.redfox.authserver.rest

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class LogoutController {

  @GetMapping("/api/logout")
  fun logout(
      request: HttpServletRequest,
      @RequestParam("redirect_uri", required = false) redirectUri: String?,
  ): String {
    request.session.invalidate()
    val target = redirectUri ?: "/login?logout"
    return "redirect:$target"
  }
}
