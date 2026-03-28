plugins {
    id("internal.jacoco-convention")
    id("internal.kotlin-library-convention")
}

dependencies {
    testImplementation(platform(project(":redfox-libs:redfox-bom")))

    testImplementation(project(":redfox-libs:redfox-testkit"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<ProcessResources>().configureEach {
    exclude("**/.gitkeep")
    includeEmptyDirs = false
}
