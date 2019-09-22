import java.io.ByteArrayOutputStream

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven("https://files.minecraftforge.net/maven")
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.github.MinecraftForge:ForgeGradle:4484428") {
            exclude(group = "trove", module = "trove") // preprocessor/idea requires more recent one
        }
        // classpath("net.minecraftforge.gradle:ForgeGradle:3.+") {
        //     exclude(group = "trove", module = "trove") // preprocessor/idea requires more recent one
        // }
        classpath("com.github.ReplayMod:ForgeGradle:d5c13801") {
            exclude(group = "net.sf.trove4j", module = "trove4j") // preprocessor/idea requires more recent one
            exclude(group = "trove", module = "trove") // different name same thing
        }
    }
}

plugins {
    kotlin("jvm") version "1.3.40" apply false
    id("com.replaymod.preprocess") version "4af749d" apply false
}

version = determineVersion()

fun determineVersion(): String {
    val latestVersion = file("version.txt").readLines().first()
    val releaseCommit = command("git", "blame", "-p", "-l", "version.txt").first().split(" ").first()
    val currentCommit = command("git", "rev-parse", "HEAD").first()
    val version = if (releaseCommit == currentCommit) {
        latestVersion
    } else {
        val diff = command("git", "log", "--format=oneline", "$releaseCommit..$currentCommit").size
        "$latestVersion-$diff-g${currentCommit.substring(0, 7)}"
    }
    return if (command("git", "status", "--porcelain").any { it.isNotEmpty() }) {
        "$version*"
    } else {
        version
    }
}

fun command(vararg cmd: Any): List<String> {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine(*cmd)
        standardOutput = stdout
    }
    return stdout.toString().lines()
}

project("1.12.2") {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "net.minecraftforge.gradle.forge")
    apply(plugin = "com.replaymod.preprocess")
}

project("1.14.4") {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "net.minecraftforge.gradle")
    apply(plugin = "com.replaymod.preprocess")
}
