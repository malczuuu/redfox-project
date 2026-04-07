package io.github.malczuuu.redfox.app.security

data class TokenDto(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Int,
)
