package io.github.malczuuu.redfox.app.core

import io.github.malczuuu.redfox.app.common.ConflictException
import io.github.malczuuu.redfox.app.common.NotFoundException

class ProjectNotFoundException : NotFoundException(projectNotFoundDetail)

class ProjectAlreadyExistsException : ConflictException(projectAlreadyExists)

class ThingNotFoundException : NotFoundException(thingNotFoundDetail)

class ThingAlreadyExistsException : ConflictException(thingAlreadyExists)

class UserNotFoundException : NotFoundException(userNotFoundDetail)

class UserAlreadyExistsException : ConflictException(userAlreadyExists)

private const val notFoundDetail = "not found"
private const val alreadyExistsDetail = "already exists"

private const val projectNotFoundDetail = "project $notFoundDetail"
private const val projectAlreadyExists = "project $alreadyExistsDetail"

private const val thingNotFoundDetail = "thing $notFoundDetail"
private const val thingAlreadyExists = "thing $alreadyExistsDetail"

private const val userNotFoundDetail = "user $notFoundDetail"
private const val userAlreadyExists = "user $alreadyExistsDetail"
