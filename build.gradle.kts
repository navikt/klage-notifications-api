import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val testContainersVersion = "2.0.3"
val klageKodeverkVersion = "1.12.16"
val springMockkVersion = "5.0.1"
val mockkVersion = "1.14.9"
val tokenValidationVersion = "6.0.0"
val logstashVersion = "9.0"
val reactorKafkaVersion = "1.3.25"
val springDocVersion = "3.0.1"
val shedlockVersion = "7.6.0"
val otelVersion = "1.59.0"

plugins {
    val kotlinVersion = "2.3.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// CVE GHSA-72hv-8253-57qq: jackson-core async parser DoS. Remove when Spring has updated.
extra["jackson-2-bom.version"] = "2.21.1"
extra["jackson-bom.version"] = "3.1.0"

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

// Remove when Spring has updated.
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.lz4:lz4-java"))
            .using(module("at.yawk.lz4:lz4-java:1.10.1"))
            .because("CVE-2025-12183 and CVE-2025-66566: org.lz4:lz4-java is archived, new releases under at.yawk.lz4")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    modules {
        module("org.springframework.boot:spring-boot-starter-tomcat") {
            replacedBy("org.springframework.boot:spring-boot-starter-jetty")
        }
    }
    implementation("org.eclipse.jetty.http2:jetty-http2-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.projectreactor.kafka:reactor-kafka:$reactorKafkaVersion")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.zaxxer:HikariCP")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("net.logstash.logback:logstash-logback-encoder:${logstashVersion}")
    implementation("no.nav.klage:klage-kodeverk:$klageKodeverkVersion")
    implementation("no.nav.security:token-validation-spring:${tokenValidationVersion}")
    implementation("no.nav.security:token-client-spring:${tokenValidationVersion}")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.opentelemetry:opentelemetry-api:${otelVersion}")
    implementation("net.javacrumbs.shedlock:shedlock-spring:${shedlockVersion}")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:${shedlockVersion}")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage")
        exclude(group = "org.mockito")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test") {
        exclude(group = "org.junit.vintage")
        exclude(group = "org.mockito")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers:${testContainersVersion}")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:${testContainersVersion}")
    testImplementation("org.testcontainers:testcontainers-postgresql:${testContainersVersion}")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("com.ninja-squad:springmockk:${springMockkVersion}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}