package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.api.ThingService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/projects/{projectId}/things")
class ThingController(private val thingService: ThingService) {}
