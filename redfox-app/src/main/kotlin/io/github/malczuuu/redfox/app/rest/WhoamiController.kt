package io.github.malczuuu.redfox.app.rest

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/whoami")
class WhoamiController {

  @GetMapping
  fun whoami(@AuthenticationPrincipal principal: Jwt?): ResponseEntity<Map<String, Any?>> {
    val responseBody = mutableMapOf<String, Any?>()
    principal?.let { responseBody["login"] = principal.claims["sub"] }
    return ResponseEntity.ok(responseBody)
  }
}
