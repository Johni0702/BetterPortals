package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.view.impl.net.Transaction
import io.kotlintest.*
import io.kotlintest.extensions.TestListener
import net.minecraft.client.Minecraft
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.lwjgl.opengl.Display
import java.io.PrintWriter
import java.time.Duration

lateinit var mc: Minecraft

fun runTests(mcIn: Minecraft): Boolean {
    mc = mcIn

    mc.gameSettings.showDebugInfo = true
    mc.gameSettings.pauseOnLostFocus = false
    Transaction.disableTransactions = true

    Display.getDrawable().releaseContext()
    System.setProperty("kotlintest.project.config", ProjectConfig::class.java.name)

    val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(SinglePortalTraversalTests::class.java))
            .selectors(selectClass(NearTeleporterTraversalTests::class.java))
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
    Display.getDrawable().makeCurrent()
    (mc as IHasMainThread).setMainThread()
    (mc.integratedServer as IHasMainThread?)?.setMainThread()
}

fun releaseMainThread() {
    Display.getDrawable().releaseContext()
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
                    // Initial screenshot
                    renderToScreenshot(testCase.description.fullName() + ".first.png")

                    // Extra render passes to get lazily computed things updated
                    repeat(5) { render() }
                    renderToScreenshot(testCase.description.fullName() + ".last.png")

                    // Debug view
                    BPConfig.debugView = true
                    renderToScreenshot(testCase.description.fullName() + ".debug.png")
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
