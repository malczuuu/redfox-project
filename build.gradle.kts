import com.diffplug.spotless.LineEnding

plugins {
    id("internal.common-convention")
    id("internal.idea-convention")
    id("jacoco-report-aggregation")
    id("test-report-aggregation")
    alias(libs.plugins.spotless)
}

dependencies {
    jacocoAggregation(project(":redfox-app"))
    jacocoAggregation(project(":redfox-authserver"))
    jacocoAggregation(project(":redfox-flyway"))
    jacocoAggregation(project(":redfox-gateway"))
    jacocoAggregation(project(":redfox-libs:redfox-log4j2"))
    jacocoAggregation(project(":redfox-libs:redfox-migration"))
    jacocoAggregation(project(":redfox-libs:redfox-testkit"))

    testReportAggregation(project(":redfox-app"))
    testReportAggregation(project(":redfox-authserver"))
    testReportAggregation(project(":redfox-flyway"))
    testReportAggregation(project(":redfox-gateway"))
    testReportAggregation(project(":redfox-libs:redfox-log4j2"))
    testReportAggregation(project(":redfox-libs:redfox-migration"))
    testReportAggregation(project(":redfox-libs:redfox-testkit"))
}

reporting {
    reports {
        register<JacocoCoverageReport>("testCodeCoverageReport") {
            testSuiteName = "test"
        }

        register<AggregateTestReport>("testAggregateTestReport") {
            testSuiteName = "test"
        }
    }
}

spotless {
    java {
        target("**/src/**/*.java")

        googleJavaFormat("1.35.0")
        forbidWildcardImports()
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    sql {
        target("**/src/main/resources/**/*.sql")

        dbeaver()
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    kotlin {
        target("**/src/**/*.kt")

        ktfmt("0.61").configure {
            it.setMaxWidth(100)
            it.setRemoveUnusedImports(true)
        }
        endWithNewline()
        toggleOffOn()
        lineEndings = LineEnding.UNIX
    }

    kotlinGradle {
        target("*.gradle.kts", "buildSrc/*.gradle.kts", "buildSrc/src/**/*.gradle.kts")
        targetExclude("**/build/**")

        ktlint("1.8.0").editorConfigOverride(mapOf("max_line_length" to "120"))
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    format("yaml") {
        target("**/*.yml", "**/*.yaml")
        targetExclude("**/build/**")

        trimTrailingWhitespace()
        leadingTabsToSpaces(2)
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    format("misc") {
        target("**/.gitattributes", "**/.gitignore")

        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
}

tasks.named<Task>("check") {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
    dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
}

defaultTasks("spotlessApply", "build")
