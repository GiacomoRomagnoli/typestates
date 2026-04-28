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
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

