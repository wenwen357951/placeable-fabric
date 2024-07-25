plugins {
    alias(libs.plugins.fabric.loom)
    id("maven-publish")
}

group = project.findProperty("maven_group")!!
version = project.findProperty("mod_version")!!

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

base {
    archivesName = project.findProperty("archives_base_name").toString()
}

// To change the versions see the libs.versions.toml
dependencies {
    /** Minecraft **/
    minecraft(libs.minecraft)

    /** Fabric **/
    mappings(libs.fabric.yarn)
    modImplementation(libs.fabric.loader)
    // Fabric API
    modImplementation(libs.fabric.api)
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${project.base.archivesName.get()}" }
        }
    }
}
