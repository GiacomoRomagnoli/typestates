plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "it.unibo.typestates"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":annotations"))
    testImplementation(libs.compile.testing)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
