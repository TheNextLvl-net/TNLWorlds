import io.papermc.hangarpublishplugin.model.Platforms
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("io.papermc.paperweight.userdev") version "1.7.7"
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("io.github.goooler.shadow") version "8.1.8"
    id("com.modrinth.minotaur") version "2.+"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.compileJava {
    options.release.set(21)
}

group = project(":api").group
version = project(":api").version

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.thenextlvl.net/releases")
    maven("https://repo.thenextlvl.net/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://github.com/Folia-Inquisitors/FoliaDevBundle/raw/gh-pages/")
}

dependencies {
    paperweight.foliaDevBundle("1.21.1-R0.1-SNAPSHOT")

    compileOnly("org.projectlombok:lombok:1.18.36")

    compileOnly("net.thenextlvl.core:annotations:2.0.1")

    implementation("org.bstats:bstats-bukkit:3.1.0")

    implementation(project(":api"))
    implementation("net.thenextlvl.core:nbt:2.2.14")
    implementation("net.thenextlvl.core:files:2.0.0")
    implementation("net.thenextlvl.core:i18n:1.0.20")
    implementation("net.thenextlvl.core:paper:2.0.2")
    implementation("net.thenextlvl.core:adapters:2.0.1")

    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

tasks.shadowJar {
    relocate("org.bstats", "net.thenextlvl.worlds.bstats")
    archiveBaseName.set("worlds")
}

paper {
    name = "Worlds"
    main = "net.thenextlvl.worlds.WorldsPlugin"
    apiVersion = "1.20"
    description = "Create, delete and manage your worlds"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    website = "https://thenextlvl.net"
    authors = listOf("NonSwag")
    foliaSupported = true

    permissions {
        register("worlds.commands.admin") {
            this.children = listOf(
                "worlds.command.clone",
                "worlds.command.create",
                "worlds.command.delete",
                "worlds.command.import",
                "worlds.command.info",
                "worlds.command.link",
                "worlds.command.list",
                "worlds.command.load",
                "worlds.command.save",
                "worlds.command.save-all",
                "worlds.command.save-off",
                "worlds.command.save-on",
                "worlds.command.setspawn",
                "worlds.command.spawn",
                "worlds.command.teleport",
                "worlds.command.unload",
            )
        }
        register("worlds.commands.link") {
            this.children = listOf(
                "worlds.command.link.create",
                "worlds.command.link.delete",
                "worlds.command.link.list"
            )
        }
        register("worlds.command.link.create") {
            this.children = listOf("worlds.command.link")
        }
        register("worlds.command.link.delete") {
            this.children = listOf("worlds.command.link")
        }
        register("worlds.command.link.list") {
            this.children = listOf("worlds.command.link")
        }

        register("worlds.command.link") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.clone") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.create") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.delete") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.import") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.info") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.list") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.load") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.save") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.save-all") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.save-off") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.save-on") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.setspawn") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.spawn") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.teleport") {
            this.children = listOf("worlds.command")
        }
        register("worlds.command.unload") {
            this.children = listOf("worlds.command")
        }
    }
}

val versionString: String = project.version as String
val isRelease: Boolean = !versionString.contains("-pre")

val versions: List<String> = (property("gameVersions") as String)
    .split(",")
    .map { it.trim() }

hangarPublish { // docs - https://docs.papermc.io/misc/hangar-publishing
    publications.register("plugin") {
        id.set("Worlds")
        version.set(versionString)
        channel.set(if (isRelease) "Release" else "Snapshot")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms.register(Platforms.PAPER) {
            jar.set(tasks.shadowJar.flatMap { it.archiveFile })
            platformVersions.set(versions)
        }
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("gBIw3Gvy")
    versionType = if (isRelease) "release" else "beta"
    uploadFile.set(tasks.shadowJar)
    gameVersions.set(versions)
    loaders.add("paper")
}