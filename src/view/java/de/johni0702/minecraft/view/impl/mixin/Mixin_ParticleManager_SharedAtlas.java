//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.view.impl.client.ClientState;
//$$ import net.minecraft.client.particle.IParticleFactory;
//$$ import net.minecraft.client.particle.ParticleManager;
//$$ import net.minecraft.client.renderer.texture.AtlasTexture;
//$$ import net.minecraft.client.renderer.texture.ITickableTextureObject;
//$$ import net.minecraft.client.renderer.texture.TextureManager;
//$$ import net.minecraft.util.ResourceLocation;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Mutable;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ import java.util.Map;
//$$
//#if FABRIC>=1
//$$ import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
//#else
//#endif
//$$
//$$ /**
//$$  * We want to share atlas, sprites and factories between multiple instances so we only need to load them once.
//$$  */
//$$ @Mixin(ParticleManager.class)
//$$ public abstract class Mixin_ParticleManager_SharedAtlas implements ClientState.IParticleManager {
//$$     private static ParticleManager overwrite;
//$$
    //#if FABRIC>=1
    //$$ @Shadow @Final @Mutable private Int2ObjectMap<ParticleFactory<?>> factories;
    //#else
    //$$ @Shadow @Final @Mutable private Map<ResourceLocation, IParticleFactory<?>> factories;
    //#endif
//$$     @Shadow @Final @Mutable private Map sprites;
//$$     @Shadow @Final @Mutable private AtlasTexture atlas;
//$$     @Shadow @Final private TextureManager renderer;
//$$
//$$     @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;loadTickableTexture(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/ITickableTextureObject;)Z"))
//$$     private boolean setupSharedAtlas(TextureManager textureManager, ResourceLocation textureLocation, ITickableTextureObject textureObj) {
//$$         Mixin_ParticleManager_SharedAtlas overwrite = (Mixin_ParticleManager_SharedAtlas) (Object) Mixin_ParticleManager_SharedAtlas.overwrite;
//$$         if (overwrite != null) {
//$$             this.factories = overwrite.factories;
//$$             this.sprites = overwrite.sprites;
//$$             this.atlas = overwrite.atlas;
//$$             return true;
//$$         } else {
//$$             return textureManager.loadTickableTexture(textureLocation, textureObj);
//$$         }
//$$     }
//$$
//$$     @Inject(method = "registerFactories", at = @At("HEAD"), cancellable = true)
//$$     private void skipFactoriesWhenAtlasShared(CallbackInfo ci) {
//$$         if (overwrite != null) {
//$$             ci.cancel();
//$$         }
//$$     }
//$$
//$$     @NotNull
//$$     @Override
//$$     public ParticleManager createWithSharedAtlas() {
//$$         overwrite = (ParticleManager) (Object) this;
//$$         try {
//$$             return new ParticleManager(null, this.renderer);
//$$         } finally {
//$$             overwrite = null;
//$$         }
//$$     }
//$$ }
//#endif
