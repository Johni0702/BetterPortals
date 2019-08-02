package de.johni0702.minecraft.betterportals.impl

import net.minecraft.launchwrapper.Launch
import net.minecraftforge.fml.relauncher.CoreModManager
import org.spongepowered.asm.mixin.Mixins
import java.io.File

internal class TestLoader : MixinLoader(File("../../")) {
    init {
        Launch.classLoader.addURL(File(root, "src/test/resources").toURI().toURL())
        Mixins.addConfiguration("mixins.betterportals.test.json")

        // I don't understand Forge's logic for putting the BP jar file in there but it apparently is when running from
        // gradle via :runIntegrationTest and that prevents BP from being loaded...
        CoreModManager.getReparseableCoremods().apply {
            assert(size == 1) // it's the only file, so we can just clear
            clear()
        }
    }
}
