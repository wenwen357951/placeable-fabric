plugins {
    id("maven-publish")
    alias(libs.plugins.fabric.loom)
}

group = "it.bisumto"
version = "1.0.4"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
}

repositories {
    maven {
        name = "Masa"
        url = uri("https://masa.dy.fi/maven")
    }
}

dependencies {
    // To change the versions see the libs.versions.toml
    minecraft(libs.minecraft)
    mappings(libs.fabric.yarn)
    modImplementation(libs.fabric.loader)
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        from("LICENSE")
    }
}
