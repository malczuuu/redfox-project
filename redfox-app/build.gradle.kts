plugins {
    id("internal.jacoco-convention")
    id("internal.kotlin-spring-app-convention")
}

dependencies {
    annotationProcessor(platform(project(":redfox-libs:redfox-bom")))
    annotationProcessor(libs.spring.boot.configuration.processor)

    implementation(platform(project(":redfox-libs:redfox-bom")))

    implementation(project(":redfox-libs:redfox-log4j2"))

    implementation(libs.tools.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    implementation(libs.problem4j.spring.webmvc)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.log4j2)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.micrometer.registry.prometheus)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    runtimeOnly(libs.postgresql)

    testImplementation(project(":redfox-libs:redfox-migration"))
    testImplementation(project(":redfox-libs:redfox-testkit"))
    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.postgresql)
}

configurations.all {
    exclude(group = libs.spring.boot.starter.logging.get().group, module = libs.spring.boot.starter.logging.get().name)
}
