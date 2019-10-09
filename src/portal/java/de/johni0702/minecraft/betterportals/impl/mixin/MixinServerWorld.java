//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.betterportals.impl.EntityEventEmitter;
//$$ import kotlin.Unit;
//$$ import kotlin.jvm.functions.Function1;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.world.server.ServerWorld;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Group;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ import java.util.ArrayList;
//$$ import java.util.List;
//$$
//$$ @Mixin(ServerWorld.class)
//$$ public abstract class MixinServerWorld implements EntityEventEmitter {
//$$     private final List<Function1<? super Entity, Unit>> onEntityAdded = new ArrayList<>();
//$$     private final List<Function1<? super Entity, Unit>> onEntityRemoved = new ArrayList<>();
//$$
//$$     @Override
//$$     public void addEntitiesListener(@NotNull Function1<? super Entity, Unit> onEntityAdded, @NotNull Function1<? super Entity, Unit> onEntityRemoved) {
//$$         this.onEntityAdded.add(onEntityAdded);
//$$         this.onEntityRemoved.add(onEntityRemoved);
//$$     }
//$$
//$$     @Inject(method = "onEntityAdded", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onAddedToWorld()V"))
//$$     private void onEntityAdded(Entity entity, CallbackInfo ci) {
//$$         onEntityAdded.forEach(it -> it.invoke(entity));
//$$     }
//$$
//$$     @Group(name ="onEntityRemoved")
//$$     @Inject(method = "onEntityRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onRemovedFromWorld()V"))
//$$     private void onEntityRemoved_Vanilla(Entity entity, CallbackInfo ci) {
//$$         onEntityRemoved.forEach(it -> it.invoke(entity));
//$$     }
//$$
//$$     @Group(name = "onEntityRemoved")
//$$     @Inject(method = "removeEntityComplete", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onRemovedFromWorld()V"), remap = false)
//$$     private void onEntityRemoved_Forge(Entity entity, boolean keepData, CallbackInfo ci) {
//$$         onEntityRemoved.forEach(it -> it.invoke(entity));
//$$     }
//$$ }
//#endif
