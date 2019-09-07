import net.minecraftforge.gradle.tasks.GenSrgs
import net.minecraftforge.gradle.user.TaskSingleReobf
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.3.40"
    id("net.minecraftforge.gradle.forge")
}

version = determineVersion()
group = "de.johni0702.minecraft"

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

val implementations = mapOf(
        "view" to listOf(),
        "transition" to listOf(),
        "portal" to listOf(),
        "vanilla" to listOf(),
        "twilightforest" to listOf("the-twilight-forest:twilightforest-1.12.2:3.9.984:universal"),
        "mekanism" to listOf(
                "mekanism:Mekanism:1.12.2:9.8.1.383",
                "redstone-flux:RedstoneFlux-1.12:2.1.0.6:universal",
                "industrial-craft:Industrialcraft-2-2.8.111:ex112:api"
        ),
        "aether" to listOf("the-aether:aether_legacy:1.12.2:v1.4.4"),
        "abyssalcraft" to listOf("abyssalcraft:AbyssalCraft:1.12.2:1.9.11"),
        "travelhuts" to listOf("travel-huts:travelhut:3.0.2")
)

val sourceSets = the<SourceSetContainer>()
val api by sourceSets.getting // created by ForgeGradle
for (name in implementations.keys) {
    sourceSets.register(name) {
        compileClasspath += api.compileClasspath
        compileClasspath += api.output
    }
}
val main by sourceSets.existing {
    for (name in implementations.keys) {
        compileClasspath += sourceSets[name].output
    }
}
val integrationTest by sourceSets.registering {
    compileClasspath += main.get().compileClasspath
    compileClasspath += main.get().output
}

configure<ForgeExtension> {
    // Note: For development, either uncomment the following line or manually add -Dfml.coreMods.load=de.johni0702.minecraft.betterportals.impl.MixinLoader
    //       to the jvm arguments.
    // coreMod = "de.johni0702.minecraft.betterportals.impl.MixinLoader"
    version = "1.12.2-14.23.5.2838"
    runDir = "run"
    mappings = "snapshot_20171003"
}

val mixinBaseSrgFile = project.file("build/mcp-srg.srg")
val mixinExtraSrgFile = File(project.rootDir, "extra.srg")
val mixinRefMaps = mapOf(
        "view" to File(project.buildDir, "tmp/mixins/mixins.betterportals.view.refmap.json"),
        "transition" to File(project.buildDir, "tmp/mixins/mixins.betterportals.transition.refmap.json"),
        "portal" to File(project.buildDir, "tmp/mixins/mixins.betterportals.refmap.json"),
        "integrationTest" to File(project.buildDir, "tmp/mixins/mixins.betterportals.test.refmap.json")
)
mixinRefMaps.forEach { (name, refMap) ->
    val mixinSrg = File(project.buildDir, "tmp/mixins/mixins.$name.srg")
    if (name != "integrationTest") {
        tasks.getByName<TaskSingleReobf>("reobfJar").addSecondarySrgFile(mixinSrg)
    }
    tasks.named<JavaCompile>("compile${name.capitalize()}Java") {
        dependsOn("copySrg")
        options.compilerArgs.addAll(listOf(
            "-AoutSrgFile=${mixinSrg.canonicalPath}",
            "-AoutRefMapFile=${refMap.canonicalPath}",
            "-AreobfSrgFiles=${mixinBaseSrgFile.canonicalPath};${mixinExtraSrgFile.canonicalPath}"
        ))
    }
}

repositories {
    mavenCentral()
    jcenter()
    maven("http://repo.spongepowered.org/maven/")
    maven("https://minecraft.curseforge.com/api/maven/")
    maven("https://maven.shadowfacts.net")
}

configurations {
    register("mixin") // for shading
}

// We want FG to deobf our mod-deps for us.
// However FG sucks and doesn't support non-standard source sets, so we need to use this workaround...
// Might need to run `./gradlew deobfCompileDummyTask` before importing into IDEA (doesn't happen automatically).
fun DependencyHandler.deobf(dep: String): Dependency {
    val withoutClassifier = if (dep.count { it == ':' } > 2) {
        dep.take(dep.lastIndexOf(':'))
    } else {
        dep
    }
    add("deobfCompile", dep)
    add("runtime", "deobf.$withoutClassifier")
    return create("deobf.$withoutClassifier")
}

dependencies {
    "compile"("net.shadowfacts:Forgelin:1.8.3")

    val mixinDep = "org.spongepowered:mixin:0.7.11-SNAPSHOT"
    val withoutOldMixinDeps: ModuleDependency.() -> Unit = {
        exclude(group = "com.google.guava") // 17.0
        exclude(group = "com.google.code.gson") // 2.2.4
    }
    "runtime"(mixinDep, withoutOldMixinDeps)
    "viewCompile"(mixinDep, withoutOldMixinDeps)
    "portalCompile"(mixinDep, withoutOldMixinDeps)
    "viewAnnotationProcessor"(mixinDep)
    "portalAnnotationProcessor"(mixinDep)
    "transitionAnnotationProcessor"(mixinDep)
    "mixin"(mixinDep) { isTransitive = false }

    "viewCompile"(deobf("opencubicchunks:CubicChunks-1.12.2-0.0.970.0:SNAPSHOT:all"))

    for ((name, dependencies) in implementations) {
        for (dependency in dependencies) {
            "${name}Compile"(deobf(dependency))
        }
    }

    "integrationTestCompile"("org.junit.jupiter:junit-jupiter-engine:5.5.1")
    "integrationTestCompile"("org.junit.platform:junit-platform-launcher:1.5.0")
    "integrationTestCompile"("io.kotlintest:kotlintest-runner-junit5:3.4.0")
    "integrationTestRuntimeOnly"(configurations["forgeGradleGradleStart"])
}

tasks.named<ProcessResources>("processResources") {
    // this will ensure that this task is redone when the versions change.
    inputs.property("version", project.version)
    inputs.property("mcversion", project.the<ForgeExtension>().version)

    // replace stuff in mcmod.info, nothing else
    from(main.get().resources.srcDirs) {
        include("mcmod.info")
                
        // replace version and mcversion
        expand("version" to project.version, "mcversion" to project.the<ForgeExtension>().version)
    }
        
    // copy everything else except the mcmod.info
    from(main.get().resources.srcDirs) {
        exclude("mcmod.info")
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("betterportals")
    from({ configurations["mixin"].files.map { project.zipTree(it) } }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.RSA")
    }
    from(api.output)
    from(implementations.keys.map { sourceSets[it].output })
    exclude("de/johni0702/minecraft/betterportals/MixinLoader.class")
    exclude("net/optifine") // skeletons
    from(mixinRefMaps.filterNot { it.key == "integrationTest" }.values)
    manifest {
        attributes(
                "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                "TweakOrder" to "0",
                "ForceLoadAsMod" to "true",
                "FMLCorePluginContainsFMLMod" to "true",
                "MixinConfigs" to listOf(
                        "mixins.betterportals.json",
                        "mixins.betterportals.view.json",
                        "mixins.betterportals.transition.json"
                ).joinToString(","),
                "FMLAT" to "betterportals_at.cfg"
        )
    }
}

val copySrg by tasks.registering(Copy::class) {
    dependsOn("genSrgs")
    from({ tasks.getByName<GenSrgs>("genSrgs").mcpToSrg })
    into("build")
}

tasks["setupDecompWorkspace"].dependsOn(copySrg)
tasks["setupDevWorkspace"].dependsOn(copySrg)
tasks["idea"].dependsOn(copySrg)

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val runIntegrationTest by tasks.registering(JavaExec::class) {
    dependsOn(tasks["jar"])
    dependsOn(tasks["makeStart"])

    main = "GradleStart"
    standardOutput = System.out
    errorOutput = System.err
    enableAssertions = true

    val testDir = File(project.buildDir, "integration-test")
    args( "--gameDir", testDir.canonicalPath)
    workingDir(testDir)
    doFirst {
        testDir.deleteRecursively()
        testDir.mkdirs()
    }

    // Default MC/Forge deps
    classpath(configurations["runtime"])
    classpath(configurations["forgeGradleMc"])
    classpath(configurations["forgeGradleMcDeps"])
    classpath(configurations["forgeGradleGradleStart"])
    // Base mod
    classpath(tasks.getByName<Jar>("jar").archiveFile)
    // Test classes and deps
    classpath(integrationTest.get().output)
    classpath(configurations["integrationTestCompile"])

    systemProperty("fml.noGrab", "true")
    systemProperty("fml.coreMods.load", "de.johni0702.minecraft.betterportals.impl.TestLoader")
}

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
