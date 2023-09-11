plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"

    application

    id("org.jmailen.kotlinter") version "3.13.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

application {
    mainClass.set("eternityii.AppKt")
}

detekt {
    buildUponDefaultConfig = true
    config = files("../config/detekt/detekt-config.yml")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}
