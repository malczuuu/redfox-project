package io.github.malczuuu.redfox.app.common

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
)

data class Identity(val id: String)
