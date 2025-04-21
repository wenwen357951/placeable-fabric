plugins {
    alias(libs.plugins.fabric.loom)
}

group = project.findProperty("maven_group")!!
version = project.findProperty("mod_version")!!

repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
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

    /** Third Party **/
    modApi(libs.cloth.config.fabric)
    modApi(libs.modmenu)

    /** Library **/
    implementation(libs.gson)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

base {
    archivesName = project.property("archives_base_name").toString()
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
