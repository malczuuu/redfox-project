plugins {
    id("internal.kotlin-convention")
    id("org.jetbrains.kotlin.plugin.spring")
    id("java-library")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
