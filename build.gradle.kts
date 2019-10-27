import net.minecraftforge.gradle.tasks.GenSrgs
import net.minecraftforge.gradle.user.TaskSingleReobf
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import net.minecraftforge.gradle.userdev.DependencyManagementExtension
import net.minecraftforge.gradle.userdev.UserDevExtension
import net.minecraftforge.gradle.userdev.tasks.GenerateSRG
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mcVersion = project.extra["mcVersion"] as Int
val mc11202 = mcVersion == 11202
val fg3 = mcVersion >= 11300
val mcVersionStr = "${mcVersion / 10000}.${mcVersion / 100 % 100}" + (mcVersion % 100).let { if (it == 0) "" else ".$it" }
version = parent!!.version
group = "de.johni0702.minecraft"

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

class Implementations : HashMap<String, MutableList<String>>() {
    operator fun String.invoke(run: MutableList<String>.() -> Unit) = run(getOrPut(this, ::mutableListOf))
    operator fun invoke(run: Implementations.() -> Unit) = run()
}
val implementations = Implementations()

implementations {
    "view" {}
    "transition" {}
    "portal" {}
    "vanilla" {}
    "twilightforest" {
        if (mc11202) add("the-twilight-forest:twilightforest-1.12.2:3.9.984:universal")
    }
    "mekanism" {
        if (mc11202) add("mekanism:Mekanism:1.12.2:9.8.1.383")
        if (mc11202) add("redstone-flux:RedstoneFlux-1.12:2.1.0.6:universal")
        if (mc11202) add("industrial-craft:Industrialcraft-2-2.8.111:ex112:api")
    }
    "aether" {
        if (mc11202) add("the-aether:aether_legacy:1.12.2:v1.4.4")
    }
    "abyssalcraft" {
        if (mc11202) add("abyssalcraft:AbyssalCraft:1.12.2:1.9.11")
    }
    "travelhuts" {
        if (mc11202) add("travel-huts:travelhut:3.0.2")
    }
}

val sourceSets = the<SourceSetContainer>()
if (fg3) {
    val api by sourceSets.creating
    configurations[api.compileConfigurationName].extendsFrom(configurations.compile.get())
}
val api by sourceSets.getting // created by ForgeGradle
for (name in implementations.keys) {
    sourceSets.register(name) {
        compileClasspath += api.compileClasspath
        compileClasspath += api.output
    }
}
val main by sourceSets.existing {
    compileClasspath += api.compileClasspath
    compileClasspath += api.output
    for (name in implementations.keys) {
        compileClasspath += sourceSets[name].output
    }
}
val integrationTest by sourceSets.registering {
    compileClasspath += main.get().compileClasspath
    compileClasspath += main.get().output
}

if (fg3) {
    configure<UserDevExtension> {
        mappings("snapshot", "20191009-1.14.3")
        val client by runs.creating
        val server by runs.creating
        configure(listOf(client, server)) {
            workingDirectory(project.file("run"))
            args(listOf("", ".view", ".transition", ".portal").flatMap { listOf("--mixin", "mixins.betterportals$it.json") })
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")
            mods {
                register("betterportals") {
                    source(sourceSets["main"])
                    source(sourceSets["api"])
                    for (name in implementations.keys) {
                        source(sourceSets.getByName(name))
                    }
                }
            }
        }
    }
} else {
    configure<ForgeExtension> {
        // Note: For development, either uncomment the following line or manually add -Dfml.coreMods.load=de.johni0702.minecraft.betterportals.impl.MixinLoader
        //       to the jvm arguments.
        // coreMod = "de.johni0702.minecraft.betterportals.impl.MixinLoader"
        version = "1.12.2-14.23.5.2838"
        runDir = "run"
        mappings = "snapshot_20171003"
    }
}

val mixinBaseSrgFile = project.file("build/mcp-srg.srg")
val mixinExtraSrgFile = File(project.rootDir, "extra.srg")
val mixinRefMaps = mapOf(
        "api" to File(project.buildDir, "tmp/mixins/mixins.betterportals.refmap.json"),
        "view" to File(project.buildDir, "tmp/mixins/mixins.betterportals.view.refmap.json"),
        "transition" to File(project.buildDir, "tmp/mixins/mixins.betterportals.transition.refmap.json"),
        "portal" to File(project.buildDir, "tmp/mixins/mixins.betterportals.portal.refmap.json"),
        "integrationTest" to File(project.buildDir, "tmp/mixins/mixins.betterportals.test.refmap.json")
)
mixinRefMaps.forEach { (name, refMap) ->
    val mixinSrg = File(project.buildDir, "tmp/mixins/mixins.$name.srg")
    if (name != "integrationTest") {
        if (fg3) {
            // TODO
        } else {
            tasks.getByName<TaskSingleReobf>("reobfJar").addSecondarySrgFile(mixinSrg)
        }
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
    mavenLocal()
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
fun DependencyHandler.deobf(dep: String, configure: Dependency.() -> Unit = {}): Dependency {
    return if (fg3) {
        project.the<DependencyManagementExtension>().deobf(dep, closureOf(configure))
    } else {
        val withoutClassifier = if (dep.count { it == ':' } > 2) {
            dep.take(dep.lastIndexOf(':'))
        } else {
            dep
        }
        add("deobfCompile", dep, configure)
        add("runtime", "deobf.$withoutClassifier")
        create("deobf.$withoutClassifier")
    }
}

dependencies {
    if (fg3) {
        "minecraft"("net.minecraftforge:forge:1.14.4-28.1.70")

        // TODO shade
        "compile"("org.lwjgl.lwjgl:lwjgl_util:2.9.3") {
            isTransitive = false // only want vec, mat, quat, etc.
        }
    }

    "compile"("net.shadowfacts:Forgelin:1.8.3")

    val mixinDep = "org.spongepowered:mixin:" + (if (mcVersion >= 11400) { "0.8-preview-SNAPSHOT" } else { "0.7.11-SNAPSHOT" })
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

    "apiCompile"(deobf("opencubicchunks:CubicChunks-1.12.2-0.0.970.0:SNAPSHOT:all"))

    for ((name, dependencies) in implementations) {
        for (dependency in dependencies) {
            "${name}Compile"(deobf(dependency))
        }
    }

    "integrationTestCompile"("org.junit.jupiter:junit-jupiter-engine:5.5.1")
    "integrationTestCompile"("org.junit.platform:junit-platform-launcher:1.5.0")
    "integrationTestCompile"("io.kotlintest:kotlintest-runner-junit5:3.4.0")
    if (fg3) {
        // TODO
    } else {
        "integrationTestRuntimeOnly"(configurations["forgeGradleGradleStart"])
    }
}

tasks.named<ProcessResources>("processResources") {
    // this will ensure that this task is redone when the versions change.
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersionStr)

    // replace stuff in mcmod.info, nothing else
    from(main.get().resources.srcDirs) {
        include("mcmod.info")
        include("mods.toml")

        // replace version and mcversion
        expand("version" to project.version, "mcversion" to mcVersionStr)
    }
        
    // copy everything else except the mcmod.info
    from(main.get().resources.srcDirs) {
        exclude("mcmod.info")
        exclude("mods.toml")
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
    exclude("org/vivecraft") // skeletons
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
                        "mixins.betterportals.transition.json",
                        "mixins.betterportals.portal.json"
                ).joinToString(",")
        )
    }
}

val copySrg = if (fg3) {
    tasks.register("copySrg") {
        val createMcpToSrg by tasks.existing(GenerateSRG::class)
        dependsOn(createMcpToSrg)
        doLast {
            val tsrg = file(createMcpToSrg.get().output).readLines()
            val srg = mutableListOf<String>()
            var cls = ""
            for (line in tsrg) {
                if (line[0] != '\t') {
                    srg.add("CL: $line")
                    cls = line.split(" ")[0]
                } else {
                    val parts = line.substring(1).split(" ")
                    if (line.contains("(")) {
                        srg.add("MD: $cls/${parts[0]} ${parts[1]} $cls/${parts[2]} ${parts[1]}")
                    } else {
                        srg.add("FD: $cls/${parts[0]} $cls/${parts[1]}")
                    }
                }
            }
            File(project.buildDir, "mcp-srg.srg").writeText(srg.joinToString("\n"))
        }
    }
} else {
    val copySrg by tasks.registering(Copy::class) {
        dependsOn("genSrgs")
        from({ tasks.getByName<GenSrgs>("genSrgs").mcpToSrg })
        into("build")
    }
    tasks["setupDecompWorkspace"].dependsOn(copySrg)
    tasks["setupDevWorkspace"].dependsOn(copySrg)
    tasks["idea"].dependsOn(copySrg)
    copySrg
}
tasks.withType<JavaCompile>().configureEach { dependsOn(copySrg) }

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

if (!fg3) // TODO
tasks.register("runIntegrationTest", JavaExec::class) {
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
