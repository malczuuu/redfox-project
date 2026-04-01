pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

rootProject.name = "redfox-project"

include(":redfox-app")
include(":redfox-authserver")
include(":redfox-flyway")
include(":redfox-gateway")
include(":redfox-libs:redfox-bom")
include(":redfox-libs:redfox-log4j2")
include(":redfox-libs:redfox-migration")
include(":redfox-libs:redfox-testkit")

/**
 * Duplicate project names have some side effects. This function verifies that there are no duplicate project names in
 * the build. Throws an exception if duplicates are found.
 */
fun verifyProjectNameDuplicates(project: ProjectDescriptor) {
    val projectsByName = mutableMapOf<String, MutableList<ProjectDescriptor>>()

    collectProjectNames(project, projectsByName)

    val duplicates = projectsByName.filterValues { it.size > 1 }

    check(duplicates.isEmpty()) {
        buildString {
            appendLine("Duplicate project names are not allowed:")
            duplicates.forEach { (name, projects) ->
                appendLine("  '$name' used by: ${projects.joinToString { it.path }}")
            }
        }
    }
}

/**
 * Collects project names into the provided map.
 */
fun collectProjectNames(project: ProjectDescriptor, names: MutableMap<String, MutableList<ProjectDescriptor>>) {
    names.computeIfAbsent(project.name) { mutableListOf() }.add(project)
    project.children.forEach { collectProjectNames(it, names) }
}
