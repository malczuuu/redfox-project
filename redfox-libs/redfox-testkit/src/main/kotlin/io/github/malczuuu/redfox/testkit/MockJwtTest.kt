package io.github.malczuuu.redfox.testkit

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(JwtTestConfiguration::class)
annotation class MockJwtTest
