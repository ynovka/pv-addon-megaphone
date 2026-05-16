plugins {
    kotlin("jvm") version "2.3.21"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "ru.ynovka"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.plasmoverse.com/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")

    compileOnly("su.plo.voice.server:paper:2.1.7")

    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(21)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    archiveFileName.set("${project.name}-${project.version}.jar")

    destinationDirectory.set(
        file("/var/lib/featherpanel/volumes/ffd8989e-273d-4972-811c-fa8e2c48a66c/plugins/")
    )
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
