//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.betterportals.impl.EntityEventEmitter;
//$$ import kotlin.Unit;
//$$ import kotlin.jvm.functions.Function1;
//$$ import net.minecraft.client.world.ClientWorld;
//$$ import net.minecraft.entity.Entity;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ import java.util.ArrayList;
//$$ import java.util.List;
//$$
//$$ @Mixin(ClientWorld.class)
//$$ public abstract class MixinClientWorld implements EntityEventEmitter {
//$$     private final List<Function1<? super Entity, Unit>> onEntityAdded = new ArrayList<>();
//$$     private final List<Function1<? super Entity, Unit>> onEntityRemoved = new ArrayList<>();
//$$
//$$     @Override
//$$     public void addEntitiesListener(@NotNull Function1<? super Entity, Unit> onEntityAdded, @NotNull Function1<? super Entity, Unit> onEntityRemoved) {
//$$         this.onEntityAdded.add(onEntityAdded);
//$$         this.onEntityRemoved.add(onEntityRemoved);
//$$     }
//$$
//$$     @Inject(method = "addEntityImpl",
            //#if FABRIC>=1
            //$$ at = @At("RETURN"))
            //#else
            //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onAddedToWorld()V"))
            //#endif
//$$     private void onEntityAdded(int id, Entity entity, CallbackInfo ci) {
//$$         onEntityAdded.forEach(it -> it.invoke(entity));
//$$     }
//$$
//$$     @Inject(method = "removeEntity",
            //#if FABRIC>=1
            //$$ at = @At("RETURN"))
            //#else
            //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onRemovedFromWorld()V"))
            //#endif
//$$     private void onEntityRemoved(Entity entity, CallbackInfo ci) {
//$$         onEntityRemoved.forEach(it -> it.invoke(entity));
//$$     }
//$$ }
//#endif
