package de.johni0702.minecraft.betterportals.common.capability

import net.minecraft.nbt.NBTBase
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

class NoStorage<T> : Capability.IStorage<T> {
    override fun writeNBT(capability: Capability<T>, instance: T, side: EnumFacing): NBTBase? = null
    override fun readNBT(capability: Capability<T>, instance: T, side: EnumFacing, nbt: NBTBase) {}
}
