package de.johni0702.minecraft.betterportals.impl;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class BetterPortalsMixinConfigPlugin implements IMixinConfigPlugin {
    private Logger logger = LogManager.getLogger("mixin/betterportals");
    private boolean hasKotlin = Launch.classLoader.getClassBytes("kotlin.Pair") != null;
    private boolean hasOF = Launch.classLoader.getClassBytes("optifine.OptiFineForgeTweaker") != null;
    private boolean hasCC = Launch.classLoader.getClassBytes("io.github.opencubicchunks.cubicchunks.core.asm.coremod.CubicChunksCoreMod") != null;
    private boolean hasSponge = Launch.classLoader.getClassBytes("org.spongepowered.common.SpongePlatform") != null;
    private boolean hasVC = Launch.classLoader.getClassBytes("org.vivecraft.asm.VivecraftASMTransformer") != null;

    {
        if (!hasKotlin) {
            logger.error("Couldn't find kotlin.Pair class, Forgelin is probably missing, skipping all mixins!");
        }
        logger.debug("hasKotlin: " + hasKotlin);
        logger.debug("hasOF: " + hasOF);
        logger.debug("hasCC: " + hasCC);
        logger.debug("hasSponge: " + hasSponge);
        logger.debug("hasVC: " + hasVC);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!hasKotlin) return false;
        if (hasVC) {
            if (mixinClassName.endsWith("MixinEntityRenderer_NoOF")) {
                return true;
            }
            if (mixinClassName.endsWith("MixinEntityRenderer_OF")) {
                return false;
            }
        }
        if (mixinClassName.endsWith("_OF")) return hasOF;
        if (mixinClassName.endsWith("_NoOF")) return !hasOF;
        if (mixinClassName.endsWith("_CC")) return hasCC;
        if (mixinClassName.endsWith("_NoCC")) return !hasCC;
        if (mixinClassName.endsWith("_Sponge")) return hasSponge;
        if (mixinClassName.endsWith("_NoSponge")) return !hasSponge;
        if (mixinClassName.endsWith("_VC")) return hasVC;
        if (mixinClassName.endsWith("_NoVC")) return !hasVC;
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public BetterPortalsMixinConfigPlugin() throws IOException {
    }
}
