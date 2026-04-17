plugins {
    id("internal.kotlin-spring-library-convention")
}

dependencies {
    api(platform(project(":redfox-libs:redfox-bom")))

    api(libs.spring.boot.starter.flyway.test)
    api(libs.spring.boot.starter.data.jpa.test)
    api(libs.spring.boot.starter.validation.test)
    api(libs.spring.boot.starter.webmvc.test)

    api(libs.spring.boot.starter.oauth2.resource.server)
    api(libs.spring.boot.resttestclient)
    api(libs.spring.boot.testcontainers)

    api(libs.flyway.database.postgresql)
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.postgresql)

    api(libs.archunit)
    api(libs.checkmate)
    api(libs.tools.jackson.module.kotlin)
    api(libs.kotlin.reflect)
    api(libs.kotlin.test.junit5)
    api(libs.wiremock.spring.boot.standalone)

    testRuntimeOnly(libs.junit.platform.launcher)
}
