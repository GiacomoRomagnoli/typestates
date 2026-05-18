plugins {
    java
    antlr
    alias(libs.plugins.kotlin.jvm)
}

group = "it.unibo.typestates"

repositories {
    mavenCentral()
}

dependencies {
    antlr(libs.antlr)
    implementation(libs.antlr.runtime)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.compileTestKotlin {
    dependsOn(tasks.generateTestGrammarSource)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}