package io.github.malczuuu.redfox.app.rest

import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/whoami")
class WhoamiController {

  @GetMapping(produces = [APPLICATION_JSON_VALUE])
  fun whoami(@AuthenticationPrincipal principal: UserDetails): ResponseEntity<Map<String, Any?>> {
    val responseBody = mutableMapOf<String, Any?>()
    responseBody["login"] = principal.username
    return ResponseEntity.ok(responseBody)
  }
}
