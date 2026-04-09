package io.github.malczuuu.redfox.app.security

import com.fasterxml.jackson.annotation.JsonIgnore

data class TokenExchangeDto(val code: String, val redirectUri: String, val codeVerifier: String)

data class TokenDto(
    val accessToken: String,
    @JsonIgnore val refreshToken: String?,
    val expiresIn: Int,
)
