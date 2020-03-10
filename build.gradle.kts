import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.replaymod.gradle.preprocess.PreprocessExtension
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.task.RemapJarTask
import net.minecraftforge.gradle.tasks.GenSrgs
import net.minecraftforge.gradle.user.TaskSingleReobf
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import net.minecraftforge.gradle.userdev.DependencyManagementExtension
import net.minecraftforge.gradle.userdev.UserDevExtension
import net.minecraftforge.gradle.userdev.tasks.GenerateSRG
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "org.jetbrains.kotlin.jvm")
when (project.name) {
    "1.14.4-fabric" -> apply(plugin = "fabric-loom")
    "1.14.4" -> apply(plugin = "net.minecraftforge.gradle")
    "1.12.2" -> apply(plugin = "net.minecraftforge.gradle.forge")
    else -> throw IllegalArgumentException("Don't know how to apply to project named '${project.name}'")
}
apply(plugin = "com.replaymod.preprocess")

val fabric = project.name.contains("fabric")
val loom = fabric
val mcVersion = project.extra["mcVersion"] as Int
val mc11202 = mcVersion == 11202
val fg3 = mcVersion >= 11300 && !loom
val mcVersionStr = "${mcVersion / 10000}.${mcVersion / 100 % 100}" + (mcVersion % 100).let { if (it == 0) "" else ".$it" }
version = parent!!.version
group = "de.johni0702.minecraft"

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

configure<PreprocessExtension> {
    vars.put("MC", mcVersion)
    vars.put("FABRIC", if (fabric) 1 else 0)
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
    "nether" {}
    "end" {}
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
if (fg3 || loom) {
    val api by sourceSets.creating
    configurations[api.compileConfigurationName].extendsFrom(configurations["compile"])
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

if (loom) {
} else if (fg3) {
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
        "nether" to File(project.buildDir, "tmp/mixins/mixins.betterportals.nether.refmap.json"),
        "end" to File(project.buildDir, "tmp/mixins/mixins.betterportals.end.refmap.json"),
        "integrationTest" to File(project.buildDir, "tmp/mixins/mixins.betterportals.test.refmap.json")
)
if (loom) {
    afterEvaluate {
        // While loom does try to support mixin, it doesn't do so well for multiple source sets, so we'll do it ourselves
        val loom = the<LoomGradleExtension>()
        mixinRefMaps.forEach { (name, refMap) ->
            val mixinMap = File(project.buildDir, "tmp/mixins/mixins.$name.tiny")
            val compileTask = tasks.named<JavaCompile>("compile${name.capitalize()}Java") {
                outputs.file(mixinMap)
                outputs.file(refMap)
                options.compilerArgs.addAll(listOf(
                        "-AinMapFileNamedIntermediary=" + loom.mappingsProvider.MAPPINGS_TINY.canonicalPath,
                        "-AoutMapFileNamedIntermediary=" + mixinMap,
                        "-AoutRefMapFile=" + refMap.canonicalPath,
                        "-AdefaultObfuscationEnv=named:intermediary"
                ))
            }
            if (name != "integrationTest") {
                tasks.named<RemapJarTask>("remap${(if (name == "api") "core" else name).capitalize()}Jar") {
                    dependsOn(compileTask)
                    inputs.file(mixinMap)

                    // Unfortunately there doesn't seem to be any better way to configure this
                    var orgPath: File? = null
                    doFirst {
                        orgPath = loom.mappingsProvider.MAPPINGS_MIXIN_EXPORT
                        loom.mappingsProvider.MAPPINGS_MIXIN_EXPORT = mixinMap
                    }
                    doLast {
                        loom.mappingsProvider.MAPPINGS_MIXIN_EXPORT = orgPath!!
                    }
                }
            }
        }
    }
} else {
    mixinRefMaps.forEach { (name, refMap) ->
        val mixinSrg = File(project.buildDir, "tmp/mixins/mixins.$name.srg")
        if (name != "integrationTest") {
            val jarTaskName = "${if (name == "api") "core" else name}Jar"
            afterEvaluate {
                if (fg3) {
                    tasks.getByName<RenameJarInPlace>("reobf${jarTaskName.capitalize()}").extraMapping(mixinSrg)
                } else {
                    tasks.getByName<TaskSingleReobf>("reobf${jarTaskName.capitalize()}").addSecondarySrgFile(mixinSrg)
                }
            }
        }
        tasks.named<JavaCompile>("compile${name.capitalize()}Java") {
            dependsOn("copySrg")
            outputs.file(mixinSrg)
            outputs.file(refMap)
            options.compilerArgs.addAll(listOf(
                    "-AoutSrgFile=${mixinSrg.canonicalPath}",
                    "-AoutRefMapFile=${refMap.canonicalPath}",
                    "-AreobfSrgFiles=${mixinBaseSrgFile.canonicalPath};${mixinExtraSrgFile.canonicalPath}"
            ))
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven("http://repo.spongepowered.org/maven/")
    maven("https://minecraft.curseforge.com/api/maven/")
    maven("https://maven.shadowfacts.net")
    maven("https://jitpack.io")
}

configurations {
    register("mixin") // for shading
    register("kottle") // for embedding
}

// We want FG to deobf our mod-deps for us.
// However FG sucks and doesn't support non-standard source sets, so we need to use this workaround...
// Might need to run `./gradlew deobfCompileDummyTask` before importing into IDEA (doesn't happen automatically).
fun DependencyHandler.deobf(dep: String, configure: Dependency.() -> Unit = {}): Dependency = when {
    loom -> create(dep, closureOf(configure)) // TODO not sure how to handle this with loom
    fg3 -> project.the<DependencyManagementExtension>().deobf(dep, closureOf(configure))
    else -> {
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
    if (loom) {
        "minecraft"("com.mojang:minecraft:1.14.4")
        "mappings"("net.fabricmc:yarn:1.14.4+build.14")
        "modCompile"("net.fabricmc:fabric-loader:0.6.3+build.168")
        "modCompile"("net.fabricmc.fabric-api:fabric-api:0.4.1+build.245-1.14")
        "modCompile"("net.fabricmc.fabric-api:fabric-networking-v0:0.1.5+dfdb52d6b5")
    }
    if (fg3) {
        "minecraft"("net.minecraftforge:forge:1.14.4-28.1.70")
    }
    if (mcVersion >= 11400) {
        if (fabric) {
            "include"("org.lwjgl.lwjgl:lwjgl_util:2.9.3")
        } else {
            // TODO shade
        }
        "compile"("org.lwjgl.lwjgl:lwjgl_util:2.9.3") {
            isTransitive = false // only want vec, mat, quat, etc.
        }
    }
    if (fabric) {
        // Only really using it for the shader location fix
        "include"("com.github.Ladysnake:Satin:1.3.2")
        "modCompile"("com.github.Ladysnake:Satin:1.3.2")

        "include"("me.zeroeightsix:fiber:0.8.0-1")
        "modCompile"("me.zeroeightsix:fiber:0.8.0-1")

        "include"("javax.vecmath:vecmath:1.5.2")
        "compile"("javax.vecmath:vecmath:1.5.2")

        "compile"("com.google.code.findbugs:jsr305:3.0.2") // Minecraft provides this, not sure why loom doesn't
    }

    if (fabric) {
        "modCompile"("net.fabricmc:fabric-language-kotlin:1.3.50+build.3")
    } else if (mcVersion >= 11400) {
        val kottleDep = "kottle:Kottle:1.4.0"
        "compile"(kottleDep)
        "kottle"(kottleDep) { isTransitive = false }
    } else {
        "compile"("net.shadowfacts:Forgelin:1.8.3")
    }

    if (fabric) {
        // still need to fabric's mixin to our other source sets (though we don't add it directly but instead just
        // inherit from the default configuration where loom will add its mixin dep)
        val baseConfiguration = configurations["annotationProcessor"]
        mixinRefMaps.keys.forEach { sourceSet ->
            configurations["${sourceSet}AnnotationProcessor"].extendsFrom(baseConfiguration)
        }
    } else {
        val mixinDep = "org.spongepowered:mixin:" + (if (mcVersion >= 11400) { "0.8-preview-SNAPSHOT" } else { "0.7.11-SNAPSHOT" })
        val withoutOldMixinDeps: ModuleDependency.() -> Unit = {
            exclude(group = "com.google.guava") // 17.0
            exclude(group = "com.google.code.gson") // 2.2.4
        }
        "runtime"(mixinDep, withoutOldMixinDeps)
        mixinRefMaps.keys.forEach { sourceSet ->
            "${sourceSet}Compile"(mixinDep, withoutOldMixinDeps)
            "${sourceSet}AnnotationProcessor"(mixinDep)
        }
        "mixin"(mixinDep) { isTransitive = false }
    }

    "apiCompile"(deobf("opencubicchunks:CubicChunks-1.12.2-0.0.970.0:SNAPSHOT:all"))

    for ((name, dependencies) in implementations) {
        for (dependency in dependencies) {
            "${name}Compile"(deobf(dependency))
        }
    }

    "integrationTestCompile"("org.junit.jupiter:junit-jupiter-engine:5.5.1")
    "integrationTestCompile"("org.junit.platform:junit-platform-launcher:1.5.0")
    "integrationTestCompile"("io.kotlintest:kotlintest-runner-junit5:3.4.0")
    if (loom) {
        // TODO
    } else if (fg3) {
        // TODO
    } else {
        "integrationTestRuntimeOnly"(configurations["forgeGradleGradleStart"])
    }
}

val implJars = mutableMapOf<String, TaskProvider<Jar>>()
for (name in implementations.keys + listOf("core")) {
    val jarTaskName = "${name}Jar"
    val jarTask = tasks.register<Jar>(jarTaskName) {
        archiveBaseName.set("betterportals-$name")
        if (loom) {
            archiveClassifier.set("dev")
        }

        inputs.property("version", project.version)
        inputs.property("mcversion", mcVersionStr)
        val expansions: Action<FileCopyDetails> = Action {
            expand("version" to project.version, "mcversion" to mcVersionStr)
        }
        val exclude: Action<FileCopyDetails> = Action {
            exclude()
        }
        fun CopySpec.configureModMetaFiles() {
            filesMatching("mcmod.info", if (!fg3 && !fabric) expansions else exclude)
            filesMatching("META-INF/mods.toml", if (fg3) expansions else exclude)
            filesMatching("fabric.mod.json", if (fabric) expansions else exclude)
        }
        if (name == "core") {
            if (!fabric && !fg3) {
                from({ configurations["mixin"].files.map { project.zipTree(it) } }) {
                    exclude("META-INF/*.SF")
                    exclude("META-INF/*.RSA")
                }
            }
            from(main.get().output) {
                exclude("de/johni0702/minecraft/betterportals/MixinLoader.class")
                configureModMetaFiles()
            }
            from(api.output)
        } else {
            from(sourceSets[name].output) {
                configureModMetaFiles()
            }
        }
        exclude("net/optifine") // skeletons
        exclude("org/vivecraft") // skeletons

        val mixinRefMap = mixinRefMaps[if (name == "core") "api" else name]
        if (mixinRefMap != null) {
            from(mixinRefMap)
            if (!fabric) {
                manifest {
                    attributes(
                            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                            "TweakOrder" to "0",
                            "ForceLoadAsMod" to "true",
                            "FMLCorePluginContainsFMLMod" to "true",
                            "MixinConfigs" to "mixins.betterportals${if (name == "core") "" else ".$name"}.json"
                    )
                }
            }
        }
    }
    implJars[name] = jarTask

    if (loom) {
        tasks.register<RemapJarTask>("remap${jarTaskName.capitalize()}") {
            archiveBaseName.set("betterportals-$name")
            input.set(jarTask.get().archiveFile)
            addNestedDependencies.set(name == "core")
        }
    } else {
        (extensions["reobf"] as NamedDomainObjectContainer<*>).create(jarTaskName)
        if (fg3) {
            // FIXME maybe?
        } else {
            val reobfTask = tasks["reobf${jarTaskName.capitalize()}"] as TaskSingleReobf
            reobfTask.classpath = sourceSets[if (name == "core") "main" else name].compileClasspath
        }
    }
}

val allJar = tasks.register<Jar>("allJar") {
    archiveBaseName.set("betterportals-all")

    val archiveFiles = mutableListOf<Provider<RegularFile>>() // For merging the mod.info files on 1.12 forge
    val jarNames = mutableListOf<String>()
    for ((name, jarTaskProvider) in implJars) {
        val jarTask = jarTaskProvider.get()
        if (loom) {
            val remapTask = tasks["remap${name.capitalize()}Jar"] as RemapJarTask
            from(remapTask.archiveFile) {
                into("META-INF/jars")
            }
            jarNames.add(remapTask.archiveFileName.get())
        } else {
            dependsOn(tasks["reobf${name.capitalize()}Jar"])
            // ModLauncher doesn't support bundle-only mods, so we explode the core jar
            // if (fg3 && name == "core") {
            // WTF FORGE, WHY IS THERE NO MENTION OF THIS IN THE DOCS: https://github.com/MinecraftForge/MinecraftForge/issues/6239
            if (fg3) {
                from({ zipTree(jarTask.archiveFile) })
            } else {
                // jar-in-jar on 1.12 forge is broken by design: even though it correctly unpacks inner jars, it never
                // deletes them, so you'll immediately run into duplicate mods issues once you update a mod (and even worse
                // it won't overwrite existing files, so for development you'll not get duplicate errors but you silently
                // get the old version each time. took me some time to notice, thanks!)
                /*
                from(jarTask.archiveFile) {
                    into("META-INF/libraries")
                }
                jarNames.add(jarTask.archiveFileName.get())
                */
                from({ zipTree(jarTask.archiveFile) }) {
                    exclude("mcmod.info")
                }
                archiveFiles.add(jarTask.archiveFile)
            }
        }
    }

    // Merge mod.info files on 1.12 forge until jar-in-jar works there (so probably forever)
    if (archiveFiles.isNotEmpty()) {
        val file = project.buildDir.resolve("tmp").resolve("mcmod.info")
        archiveFiles.forEach { dependsOn(it) }
        doFirst {
            val parser = JsonParser()
            val modInfos = JsonArray()
            for (archive in archiveFiles) {
                for (modInfo in zipTree(archive).filter { it.name == "mcmod.info" }) {
                    modInfos.addAll(parser.parse(modInfo.readText()).asJsonArray)
                }
            }
            file.writeText(modInfos.toString())
        }
        from(file)
    }

    /* Once https://github.com/MinecraftForge/MinecraftForge/issues/6239 is fixed
    if (!fabric && mcVersion >= 11400) {
        val kottleConfiguration = configurations["kottle"]
        dependsOn(kottleConfiguration)
        val kottleFile = kottleConfiguration.singleFile
        from(kottleFile) {
            into("META-INF/libraries")
        }
        jarNames.add(kottleFile.name)
    }
    */

    if (loom) {
        val tmp = project.buildDir.resolve("tmp").resolve("fabric.mod.json")
        doFirst {
            tmp.writeText(JsonObject().apply {
                addProperty("schemaVersion", 1)
                addProperty("id", "betterportals-all")
                addProperty("version", project.version.toString())
                addProperty("name", "BetterPortals (all modules)")
                add("jars", JsonArray().apply {
                    for (jarName in jarNames) {
                        add(JsonObject().apply {
                            addProperty("file", "META-INF/jars/$jarName")
                        })
                    }
                })
                add("depends", JsonObject().apply {
                    addProperty("betterportals", project.version.toString())
                })
                add("custom", JsonObject().apply {
                    addProperty("modmenu:parent", "betterportals")
                })
            }.toString())
        }
        from(tmp)
        inputs.property("version", project.version)
    } else if (fg3) {
        // need to merge ContainedDeps into core manifest once https://github.com/MinecraftForge/MinecraftForge/issues/6239 is fixed
        // and in the mean time, mixin config needs to be set here as well
        manifest {
            attributes(
                    "MixinConfigs" to (mixinRefMaps.keys.filter { it != "integrationTest" }.joinToString(",") {
                        "mixins.betterportals${if (it == "api") "" else ".$it"}.json"
                    })
            )
        }
    } else {
        /* If ContainedDeps ever works on 1.12 (doubt it given it's no longer supported):
        manifest {
            attributes(
                    "ContainedDeps" to jarNames.joinToString(" ")
            )
        }
        */ // until then:
        manifest {
            attributes(
                    "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                    "TweakOrder" to "0",
                    "ForceLoadAsMod" to "true",
                    "FMLCorePluginContainsFMLMod" to "true",
                    "MixinConfigs" to (mixinRefMaps.keys.filter { it != "integrationTest" }.joinToString(",") {
                        "mixins.betterportals${if (it == "api") "" else ".$it"}.json"
                    })
            )
        }
    }
}

if (loom) {
    tasks.named<RemapJarTask>("remapJar") {
        archiveBaseName.set("betterportals")
        addNestedDependencies.set(true)
    }
}

if (!loom) {
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
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

if (!fg3 && !loom) // TODO
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
