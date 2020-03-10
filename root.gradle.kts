import java.io.ByteArrayOutputStream

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven("https://files.minecraftforge.net/maven")
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.github.MinecraftForge:ForgeGradle:0204ce3") {
            exclude(group = "trove", module = "trove") // preprocessor/idea requires more recent one
            exclude(group = "org.eclipse.jdt") // loom requires more recent one
        }
        // classpath("net.minecraftforge.gradle:ForgeGradle:3.+") {
        //     exclude(group = "trove", module = "trove") // preprocessor/idea requires more recent one
        // }
        classpath("com.github.ReplayMod:ForgeGradle:2e95b7fe") {
            exclude(group = "net.sf.trove4j", module = "trove4j") // preprocessor/idea requires more recent one
            exclude(group = "trove", module = "trove") // different name same thing
            exclude(group = "org.eclipse.jdt") // loom requires more recent one
        }
    }
}

plugins {
    kotlin("jvm") version "1.3.40" apply false
    id("fabric-loom") version "0.2.5-SNAPSHOT" apply false
    id("com.replaymod.preprocess") version "b744ea7"
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

// Loom tries to find the active mixin version by recursing up to the root project and checking each project's
// compileClasspath and build script classpath (in that order). Since we've loom in our root project's classpath,
// loom will only find it after checking the root project's compileClasspath (which doesn't exist by default).
configurations.register("compileClasspath")

preprocess {
    "1.14.4-fabric"(11404, "yarn") {
        "1.14.4"(11404, "srg", file("versions/1.14.4-fabric-forge.txt")) {
            "1.12.2"(11202, "srg", file("versions/1.14.4/mapping.txt"))
        }
    }
}
