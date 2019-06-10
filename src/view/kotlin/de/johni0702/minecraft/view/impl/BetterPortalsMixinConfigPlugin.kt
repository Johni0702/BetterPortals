package de.johni0702.minecraft.view.impl

import net.minecraft.launchwrapper.Launch
import org.spongepowered.asm.lib.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

class BetterPortalsMixinConfigPlugin : IMixinConfigPlugin {
    private val hasOF = Launch.classLoader.getClassBytes("optifine.OptiFineForgeTweaker") != null

    override fun shouldApplyMixin(targetClassName: String?, mixinClassName: String): Boolean = when {
        mixinClassName.endsWith("_OF") -> hasOF
        mixinClassName.endsWith("_NoOF") -> !hasOF
        else -> true
    }

    override fun onLoad(mixinPackage: String) {
    }

    override fun preApply(targetClassName: String?, targetClass: ClassNode?, mixinClassName: String?, mixinInfo: IMixinInfo?) {
    }

    override fun postApply(targetClassName: String?, targetClass: ClassNode?, mixinClassName: String?, mixinInfo: IMixinInfo?) {
    }

    override fun getRefMapperConfig(): String? {
        return null
    }

    override fun getMixins(): MutableList<String>? {
        return null
    }

    override fun acceptTargets(myTargets: MutableSet<String>?, otherTargets: MutableSet<String>?) {
    }
}
