
import ca.stellardrift.build.implementationInclude
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {
    id("fabric-loom") version "0.2.6-SNAPSHOT"
    id("com.github.johnrengelman.shadow")
    id("ca.stellardrift.localization")
}

repositories {
    mavenLocal()
}

minecraft {
    refmapName = "${rootProject.name.toLowerCase()}-refmap.json"
}


val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

val minecraftVersion = "1.15.1"
dependencies {
    shade(project(":permissionsex-core")) {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
        exclude("net.kyori")
    }

    shade(project(":impl-blocks:permissionsex-hikari-config"))
    shade(project(":impl-blocks:permissionsex-smarter-text")) { isTransitive = false }
    shade("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1") { isTransitive=false }

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$minecraftVersion+build.6")
    modImplementation("net.fabricmc:fabric-loader:0.7.2+build.175")
    modImplementation("com.sk89q.worldedit:worldedit-fabric-mc$minecraftVersion:7.1.0-SNAPSHOT") { isTransitive = false }
    modImplementation("com.sk89q.worldedit:worldedit-core:7.1.0-SNAPSHOT") { isTransitive = false }

    implementationInclude("net.fabricmc.fabric-api:fabric-api:0.4.25+build.282-1.15")
    implementationInclude("net.fabricmc:fabric-language-kotlin:1.3.61+build.1")
    implementationInclude("ca.stellardrift:text-adapter-fabric:3.0.3-SNAPSHOT") {
        exclude("com.google.code.gson")
    }
    //includedImplementation("ca.stellardrift:confabricate:1.0-SNAPSHOT+3.6.1")
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

tasks.processResources {
    expand("project" to project)
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    configurations = listOf(shade)
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
    }
    archiveClassifier.set("dev-all")

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
        exclude(dependency("org.checkerframework:checker-qual:.*"))
    }

    listOf("com.zaxxer", "com.github.benmanes",
        "com.typesafe.config", "ninja.leaping.configurate", "org.slf4j",
        "org.antlr", "org.yaml", "org.apache.logging.slf4j").forEach {
        relocate(it, "$relocateRoot.$it")
    }

    manifest {
        attributes("Automatic-Module-Name" to project.name)
    }
}

val remapShadowJar = tasks.register<RemapJarTask>("remapShadowJar") {
    dependsOn(shadowJar)
    archiveClassifier.set("all")
    input.set(shadowJar.archiveFile)
    addNestedDependencies.set(true)
}

tasks.assemble.configure {
    dependsOn(shadowJar)
}

tasks.build.configure {
    dependsOn(remapShadowJar)
}

opinionated {
    publication.apply {
        val remapJar by tasks.getting(RemapJarTask::class)
        val remapSourcesJar by tasks.getting(RemapSourcesJarTask::class)
        suppressAllPomMetadataWarnings()

        artifact(tasks.jar.get()) {
            classifier = "dev"
        }
        artifact(remapJar)

        artifact(tasks.getByName("sourcesJar")) {
            builtBy(remapSourcesJar)
        }
        artifact(tasks.getByName("javadocJar"))
    }
}


