package de.johni0702.minecraft.betterportals.impl.worlds

import de.johni0702.minecraft.betterportals.impl.*
import io.kotlintest.Spec

open class EmptyWorldSetup : SetClientThreadListener() {
    override fun beforeSpec(spec: Spec) {
        asMainThread {
            super.beforeSpec(spec)
            launchServer()
        }
    }

    override fun afterSpec(spec: Spec) {
        asMainThread {
            super.afterSpec(spec)
            closeServer()
            deleteWorld()
        }
    }
}