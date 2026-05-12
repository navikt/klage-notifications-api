plugins {
    kotlin("jvm")
}

val otelVersion = "1.61.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Provided by the OTEL Java agent at runtime; use `compileOnly`.
    compileOnly("io.opentelemetry:opentelemetry-sdk:$otelVersion")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:$otelVersion")
    compileOnly("io.opentelemetry:opentelemetry-api:$otelVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.jar {
    archiveBaseName.set("otel-extension")
    archiveVersion.set("")
}
