import org.jetbrains.gradle.ext.Application
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.JUnit
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    id("org.jetbrains.gradle.plugin.idea-ext")
}

idea {
    project {
        settings {
            runConfigurations {
                create<Application>("Run [redfox-app]") {
                    mainClass = "io.github.malczuuu.redfox.app.ApplicationKt"
                    moduleName = "redfox-project.redfox-app.main"
                    workingDirectory = rootProject.rootDir.absolutePath
                    programParameters = ""
                }
                create<Application>("Run [redfox-authserver]") {
                    mainClass = "io.github.malczuuu.redfox.authserver.AuthServerApplicationKt"
                    moduleName = "redfox-project.redfox-authserver.main"
                    workingDirectory = rootProject.rootDir.absolutePath
                    programParameters = ""
                }
                create<Application>("Run [redfox-flyway]") {
                    mainClass = "io.github.malczuuu.redfox.flyway.FlywayApplicationKt"
                    moduleName = "redfox-project.redfox-flyway.main"
                    workingDirectory = rootProject.rootDir.absolutePath
                    programParameters = ""
                }
                create<Gradle>("Build [redfox-project]") {
                    taskNames = listOf("spotlessApply build")
                    projectPath = rootProject.rootDir.absolutePath
                }
                create<Gradle>("Build [redfox-project|containers]") {
                    taskNames = listOf("spotlessApply build -Pcontainers.enabled")
                    projectPath = rootProject.rootDir.absolutePath
                }
                create<Gradle>("Test [redfox-project]") {
                    taskNames = listOf("test")
                    projectPath = rootProject.rootDir.absolutePath
                }
                create<Gradle>("Test [redfox-project|containers]") {
                    taskNames = listOf("test -Pcontainers.enabled")
                    projectPath = rootProject.rootDir.absolutePath
                }
                create<Gradle>("Format Code [redfox-project]") {
                    taskNames = listOf("spotlessApply")
                    projectPath = rootProject.rootDir.absolutePath
                }
                create<JUnit>("JUnit [redfox-app]") {
                    moduleName = "redfox-project.redfox-app.test"
                    workingDirectory = rootProject.rootDir.absolutePath
                    packageName = "io.github.malczuuu.redfox.app"
                }
                create<JUnit>("JUnit [redfox-authserver]") {
                    moduleName = "redfox-project.redfox-authserver.test"
                    workingDirectory = rootProject.rootDir.absolutePath
                    packageName = "io.github.malczuuu.redfox.authserver"
                }
                create<JUnit>("JUnit [redfox-flyway]") {
                    moduleName = "redfox-project.redfox-flyway.test"
                    workingDirectory = rootProject.rootDir.absolutePath
                    packageName = "io.github.malczuuu.redfox.flyway"
                }
            }
        }
    }
}
