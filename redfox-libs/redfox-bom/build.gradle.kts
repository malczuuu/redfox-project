plugins {
    id("internal.java-platform-convention")
}

dependencies {
    api(platform(libs.fasterxml.jackson.bom))
    api(platform(libs.junit.bom))
    api(platform(libs.problem4j.spring.bom))
    api(platform(libs.spring.boot.dependencies))
    api(platform(libs.tools.jackson.bom))

    constraints {
        api(libs.archunit)
        api(libs.jspecify)
        api(libs.kotlin.reflect)
        api(libs.kotlin.test.junit5)
    }
}
