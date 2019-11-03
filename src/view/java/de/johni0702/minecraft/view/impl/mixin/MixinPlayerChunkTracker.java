//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.view.impl.server.ServerWorldManager;
//$$ import net.minecraft.world.server.TicketManager;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Overwrite;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$
//$$ @Mixin(targets = "net.minecraft.world.server.TicketManager$PlayerChunkTracker")
//$$ public abstract class MixinPlayerChunkTracker {
//$$     // FIXME preprocessor might be technically able to remap these aliases
//$$     @Shadow(aliases = {"field_215500_c", "field_17462"}) @Final private TicketManager this$0;
//$$
//$$     /**
//$$      * @reason consider views when determining source level
//$$      * @author johni0702
//$$      */
//$$     @Overwrite
//$$     // FIXME preprocessor should handle this (wait, why is the fabric method public at runtime?)
    //#if FABRIC>=1
    //$$ public int getInitialLevel(long coord) {
    //#else
    //$$ protected int getSourceLevel(long coord) {
    //#endif
//$$         return ((ServerWorldManager.ITicketManager) this$0).getSourceLevelForChunk(coord);
//$$     }
//$$ }
//#endif
