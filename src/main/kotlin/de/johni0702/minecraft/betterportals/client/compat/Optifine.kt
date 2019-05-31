package de.johni0702.minecraft.betterportals.client.compat

import net.minecraft.launchwrapper.Launch

class OptifineReflection {
    private val Shaders = Class.forName("net.optifine.shaders.Shaders")!!
    private val Shaders_shaderPackLoaded = Shaders.getDeclaredField("shaderPackLoaded")!!

    var shadersActive: Boolean
        get() = Shaders_shaderPackLoaded[null] as Boolean
        set(value) { Shaders_shaderPackLoaded[null] = value }
}

val Optifine = if (Launch.classLoader.getClassBytes("optifine.OptiFineForgeTweaker") != null) {
    OptifineReflection()
} else {
    null
}
