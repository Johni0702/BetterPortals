package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.view.impl.net.Transaction
import io.kotlintest.*
import io.kotlintest.extensions.TestListener
import net.minecraft.client.Minecraft
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.PrintWriter
import java.time.Duration

//#if FABRIC>=1
//$$ import net.fabricmc.loader.launch.common.FabricLauncherBase
//$$ import net.fabricmc.loader.util.UrlUtil
//$$ import net.minecraft.entity.EntityDimensions
//#else
import net.minecraftforge.fml.client.registry.RenderingRegistry
//#endif

//#if MC>=11400
//$$ import net.minecraft.entity.EntityClassification
//$$ import net.minecraft.util.registry.Registry
//$$ import org.lwjgl.glfw.GLFW
//$$ import org.lwjgl.opengl.GL
//$$ import org.lwjgl.opengl.GLCapabilities
//#else
import org.lwjgl.opengl.Display
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.registry.EntityRegistry
//#endif

lateinit var mc: Minecraft
//#if MC>=11400
//$$ lateinit var glCapabilities: GLCapabilities
//#endif

fun preInitTests(mcIn: Minecraft) {
    mc = mcIn

    // kotlintest loads our test classes via Class.forName(String), so we need to ensure it is loaded on the same
    // classloader as MC / our mod.
    addJarToModClassLoader("org.junit.platform.launcher.core.DefaultLauncher")
    addJarToModClassLoader("org.junit.platform.engine.discovery.DiscoverySelectors")
    addJarToModClassLoader("org.junit.jupiter.engine.JupiterTestEngine")
    addJarToModClassLoader("io.kotlintest.runner.jvm.TestDiscovery")
    addJarToModClassLoader("io.kotlintest.runner.junit5.KotlinTestEngine")
}

private fun addJarToModClassLoader(className: String) {
    //#if FABRIC>=1
    //$$ val launcher = FabricLauncherBase.getLauncher()
    //$$ val classLoader = launcher.targetClassLoader
    //$$ val fileName = className.replace('.', '/') + ".class"
    //$$ val url = classLoader.getResource(fileName)!!
    //$$ val urlSource = UrlUtil.getSource(fileName, url)
    //$$ launcher.propose(urlSource)
    //#endif
}

fun initTests() {
    //#if MC<11400
    EntityRegistry.registerModEntity(
            ResourceLocation(MOD_ID, "test_entity"),
            TestEntity::class.java,
            "test_entity",
            256,
            MOD_ID,
            256,
            1,
            false
    )
    //#endif
}

fun runTests(): Boolean {
    mc.gameSettings.showDebugInfo = true
    mc.gameSettings.pauseOnLostFocus = false
    mc.gameSettings.renderDistanceChunks = 8 // some tests depend on this specific render distance
    Transaction.disableTransactions = true

    //#if MC>=11400
    //#if FABRIC>=1
    //$$ Registry.ENTITY_TYPE.registerEntityType<TestEntity>(TestEntity.ID, { _, world -> TestEntity(world) }, EntityCategory.MISC) {
    //$$     trackable(100, 1)
    //$$     size(EntityDimensions.fixed(1f, 3f))
    //$$ }
    //#else
    //$$ // FIXME
    //#endif
    //#endif

    //#if FABRIC>=1
    //$$ registerEntityRenderer { RenderTestEntity(it) }
    //#else
    mc.renderManager.entityRenderMap[TestEntity::class.java] = RenderTestEntity(mc.renderManager)
    RenderingRegistry.registerEntityRenderingHandler(TestEntity::class.java) { RenderTestEntity(it) }
    //#endif

    //#if MC>=11400
    //$$ glCapabilities = GL.getCapabilities()
    //#endif
    releaseMainThread()
    System.setProperty("kotlintest.project.config", ProjectConfig::class.java.name)

    val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(EntityCullingTests::class.java))
            .selectors(selectClass(EntityTraversalRenderTests::class.java))
            .selectors(selectClass(SinglePortalTraversalTests::class.java))
            .selectors(selectClass(SinglePortalWithSecondNearbyTraversalTest::class.java))
            .selectors(selectClass(DoublePortalTraversalTests::class.java))
            // Requires Mekanism
            //#if MC<11400
            .selectors(selectClass(NearTeleporterTraversalTests::class.java))
            //#endif
            .build()
    val launcher = LauncherFactory.create()
    val testPlan = launcher.discover(request)
    val summaryListener = SummaryGeneratingListener()
    launcher.registerTestExecutionListeners(summaryListener)
    launcher.execute(testPlan)

    val summary = summaryListener.summary
    summary.printTo(PrintWriter(System.err))
    summary.printFailuresTo(PrintWriter(System.err))
    return summary.totalFailureCount == 0L
}

interface IHasMainThread {
    fun setMainThread()
}

object ProjectConfig : AbstractProjectConfig() {
    override val timeout: Duration?
        get() = 30.seconds
}

fun acquireMainThread() {
    //#if MC>=11400
    //$$ GLFW.glfwMakeContextCurrent(mc.mainWindow.handle)
    //$$ GL.setCapabilities(glCapabilities)
    //#else
    Display.getDrawable().makeCurrent()
    //#endif
    (mc as IHasMainThread).setMainThread()
    (mc.integratedServer as IHasMainThread?)?.setMainThread()
    //#if MC>=11400
    //$$ mc.integratedServer?.worlds?.forEach { (it.chunkProvider as IHasMainThread).setMainThread() }
    //#endif
}

fun releaseMainThread() {
    //#if MC>=11400
    //$$ GLFW.glfwMakeContextCurrent(0)
    //$$ GL.setCapabilities(null)
    //#else
    Display.getDrawable().releaseContext()
    //#endif
}

private var inAsMainThread = false
fun asMainThread(block: () -> Unit) {
    if (inAsMainThread) {
        block()
    } else {
        inAsMainThread = true
        acquireMainThread()
        try {
            block()
        } finally {
            releaseMainThread()
            inAsMainThread = false
        }
    }
}

open class SetClientThreadListener : TestListener {
    override fun beforeTest(testCase: TestCase) {
        println("Begin ${testCase.description.fullName()}")
        acquireMainThread()
        super.beforeTest(testCase)
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        when(result.status) {
            TestStatus.Error, TestStatus.Failure -> {
                println("Failed ${testCase.description.fullName()}, taking screenshot..")
                try {
                    // Previous render result (in case render tests fail)
                    screenshot(testCase.description.fullName() + ".previous.png")

                    // Initial screenshot
                    renderToScreenshot(testCase.description.fullName() + ".first.png")

                    // Extra render passes to get lazily computed things updated
                    repeat(5) { render() }
                    renderToScreenshot(testCase.description.fullName() + ".last.png")

                    // Debug view
                    BPConfig.debugView = true
                    renderToScreenshot(testCase.description.fullName() + ".debug.png")
                    BPConfig.debugView = false
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            else -> {}
        }

        super.afterTest(testCase, result)
        releaseMainThread()
        println("After ${testCase.description.fullName()}")
    }
}
