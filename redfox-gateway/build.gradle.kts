plugins {
    id("internal.jacoco-convention")
    id("internal.kotlin-spring-app-convention")
}

dependencies {
    annotationProcessor(platform(project(":redfox-libs:redfox-bom")))
    annotationProcessor(libs.spring.boot.configuration.processor)

    implementation(platform(project(":redfox-libs:redfox-bom")))
    implementation(platform(libs.spring.cloud.dependencies))

    implementation(project(":redfox-libs:redfox-log4j2"))

    implementation(libs.tools.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.log4j2)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.cloud.starter.gateway.server.webmvc")
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

configurations.all {
    exclude(group = libs.spring.boot.starter.logging.get().group, module = libs.spring.boot.starter.logging.get().name)
}
