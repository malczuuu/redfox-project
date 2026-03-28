package io.github.malczuuu.redfox.app.common

import io.github.problem4j.core.Problem
import io.github.problem4j.core.ProblemException
import org.springframework.http.HttpStatus

open class BadRequestException(detail: String) :
    ProblemException(Problem.of(HttpStatus.BAD_REQUEST.value(), detail))

open class NotFoundException(detail: String) :
    ProblemException(Problem.of(HttpStatus.NOT_FOUND.value(), detail))

open class ConflictException(detail: String) :
    ProblemException(Problem.of(HttpStatus.CONFLICT.value(), detail))
