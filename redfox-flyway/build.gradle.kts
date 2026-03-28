plugins {
    id("internal.jacoco-convention")
    id("internal.kotlin-spring-app-convention")
}

dependencies {
    implementation(platform(project(":redfox-libs:redfox-bom")))

    implementation(project(":redfox-libs:redfox-log4j2"))
    implementation(project(":redfox-libs:redfox-migration"))

    implementation(libs.tools.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    implementation(libs.flyway.database.postgresql)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.log4j2)
    runtimeOnly(libs.postgresql)

    testImplementation(project(":redfox-libs:redfox-testkit"))
    testImplementation(libs.spring.boot.starter.flyway.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.postgresql)
}

configurations.all {
    exclude(group = libs.spring.boot.starter.logging.get().group, module = libs.spring.boot.starter.logging.get().name)
}
