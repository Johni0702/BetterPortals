pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven("http://files.minecraftforge.net/maven")
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.minecraftforge.gradle.forge") {
                useModule("com.github.ReplayMod:ForgeGradle:d5c13801") // FG 2.3 with Gradle 5 patches
            }
        }
    }
}