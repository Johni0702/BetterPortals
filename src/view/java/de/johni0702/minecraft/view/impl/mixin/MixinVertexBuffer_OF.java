package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.compat.OFVertexBuffer;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VertexBuffer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinVertexBuffer_OF implements OFVertexBuffer {
}
