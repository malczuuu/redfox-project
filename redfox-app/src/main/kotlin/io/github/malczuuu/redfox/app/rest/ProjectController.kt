package io.github.malczuuu.redfox.app.rest

import io.github.malczuuu.redfox.app.api.ProjectService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(private val projectService: ProjectService) {}
