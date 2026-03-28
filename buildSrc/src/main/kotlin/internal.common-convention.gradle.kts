repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/malczuuu/checkmate")
        credentials {
            username = project.findProperty("gpr.user")?.toString() ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.token")?.toString() ?: System.getenv("GPR_TOKEN")
        }
    }
}

// Usage:
//   ./gradlew printVersion
tasks.register<DefaultTask>("printVersion") {
    description = "Prints the current project version to the console."
    group = "help"

    val projectName = project.name
    val projectVersion = project.version.toString()

    doLast {
        println("$projectName version: $projectVersion")
    }
}
