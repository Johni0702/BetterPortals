// TODO might want to place the loader hack in here if Mixin 0.8 isn't released soon
//#if MC<11400
package de.johni0702.minecraft.betterportals.impl

import net.minecraft.launchwrapper.Launch
import net.minecraftforge.fml.relauncher.CoreModManager
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import org.apache.logging.log4j.LogManager
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.Mixins
import java.io.File
import java.net.URISyntaxException

open class MixinLoader(val root: File) : IFMLLoadingPlugin {
    @Suppress("unused")
    constructor() : this(File(".."))

    init {
        // Forge appears to not support custom source sets
        listOf("api", "view", "transition", "portal").forEach {
            Launch.classLoader.addURL(File(root, "src/$it/resources").toURI().toURL())
        }

        MixinBootstrap.init()
        Mixins.addConfiguration("mixins.betterportals.json")
        Mixins.addConfiguration("mixins.betterportals.view.json")
        Mixins.addConfiguration("mixins.betterportals.transition.json")
        Mixins.addConfiguration("mixins.betterportals.portal.json")
        Mixins.addConfiguration("mixins.betterportals.nether.json")
        Mixins.addConfiguration("mixins.betterportals.end.json")

        val codeSource = javaClass.protectionDomain.codeSource
        if (codeSource != null) {
            val location = codeSource.location
            try {
                val file = File(location.toURI())
                if (file.isFile) {
                    // This forces forge to reexamine the jar file for FML mods
                    // Should eventually be handled by Mixin itself, maybe?
                    CoreModManager.getIgnoredMods().remove(file.name)
                }
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }

        } else {
            LogManager.getLogger().warn("No CodeSource, if this is not a development environment we might run into problems!")
            LogManager.getLogger().warn(javaClass.protectionDomain)
        }
    }

    override fun getASMTransformerClass(): Array<String> = arrayOf()

    override fun getModContainerClass(): String? = null

    override fun getSetupClass(): String? = null

    override fun injectData(data: Map<String, Any>) {}

    override fun getAccessTransformerClass(): String? = null
}
//#endif
