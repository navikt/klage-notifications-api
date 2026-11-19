plugins {
    kotlin("jvm")
}

val otelVersion = "1.68.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
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

tasks.jar {
    archiveBaseName.set("otel-extension")
    archiveVersion.set("")
}
