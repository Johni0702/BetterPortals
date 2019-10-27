package de.johni0702.minecraft.betterportals.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;

//#if MC>=11400
//$$ import java.io.InputStream;
//#else
import net.minecraft.launchwrapper.Launch;
//#endif

public class BetterPortalsMixinConfigPlugin implements IMixinConfigPlugin {
    private boolean hasClass(String name) throws IOException {
        //#if MC>=11400
        //$$ InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
        //$$ if (stream != null) stream.close();
        //$$ return stream != null;
        //#else
        return Launch.classLoader.getClassBytes(name) != null;
        //#endif
    }

    private Logger logger = LogManager.getLogger("mixin/betterportals");
    private boolean hasKotlin = hasClass("kotlin.Pair");
    private boolean hasOF = hasClass("optifine.OptiFineForgeTweaker");
    private boolean hasCC = hasClass("io.github.opencubicchunks.cubicchunks.core.asm.coremod.CubicChunksCoreMod");
    private boolean hasSponge = hasClass("org.spongepowered.common.SpongePlatform");
    private boolean hasVC = hasClass("org.vivecraft.asm.VivecraftASMTransformer");

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
