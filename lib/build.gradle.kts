version = "0.1.0"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    `java-library`
    kotlin("plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
}

buildscript {
    repositories {
        mavenCentral()
    }
}

dependencies {
    val ktorVersion = "2.3.6"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    val log4j2Version = "2.20.0"
    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4j2Version")
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    api("org.apache.commons:commons-math3:3.6.1")
    implementation("com.google.guava:guava:32.1.1-jre")
    implementation(kotlin("reflect"))
    implementation("com.microsoft.graph:microsoft-graph:5.77.0")
    implementation("com.azure:azure-identity:1.11.1")
    implementation("com.microsoft.azure:msal4j:1.14.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}
