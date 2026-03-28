package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.api.UserService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {}
