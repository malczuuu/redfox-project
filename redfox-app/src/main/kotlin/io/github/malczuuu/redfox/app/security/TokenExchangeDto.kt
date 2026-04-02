package io.github.malczuuu.redfox.app.security

data class TokenExchangeDto(
    val code: String,
    val redirectUri: String,
    val codeVerifier: String,
)