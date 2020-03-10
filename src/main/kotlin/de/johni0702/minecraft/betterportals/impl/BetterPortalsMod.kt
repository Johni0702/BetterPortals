package de.johni0702.minecraft.betterportals.impl

//#if FABRIC>=1
//#else
import net.minecraftforge.fml.common.Mod
//#endif

//#if MC>=11400
//#if FABRIC>=1
//#else
//$$ import net.alexwells.kottle.FMLKotlinModLoadingContext
//$$ import net.minecraftforge.eventbus.api.EventPriority
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent
//$$ import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
//#endif
//#else
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.FolderResourcePack
import net.minecraft.client.resources.IResourcePack
import net.minecraftforge.fml.common.SidedProxy
import java.io.File
//#endif

const val MOD_ID = "betterportals"

//#if FABRIC>=1
//$$ object BetterPortalsMod {
//#else
//#if MC>=11400
//$$ @Mod(MOD_ID)
//#else
@Mod(modid = MOD_ID, useMetadata = true)
//#endif
internal class BetterPortalsMod {
//#endif

    //#if FABRIC>=1
    //$$ fun init() {
    //$$     BPConfig.load()
    //$$ }
    //#else
    //#if MC>=11400
    //$$ init { FMLKotlinModLoadingContext.get().modEventBus.register(this) }
    //$$ @SubscribeEvent(priority = EventPriority.HIGH)
    //$$ fun init(event: FMLCommonSetupEvent) {
    //$$     BPConfig.load()
    //$$ }
    //#endif
    //#endif

    //#if FABRIC<1 && MC<11400
    interface Proxy

    @Suppress("unused")
    internal class ServerProxy : Proxy

    @Suppress("unused")
    internal class ClientProxy : Proxy {
        // Note: Even pre-init is too late
        init {
            // Forge appears to not be able to handle multiple source sets
            try {
                val field = Minecraft::class.java.getDeclaredField("defaultResourcePacks")
                field.isAccessible = true

                var root: File? = File(".").absoluteFile
                while (root != null && !File(root, "src").exists()) {
                    root = root.parentFile
                }

                if (root != null) {
                    val mc = Minecraft.getMinecraft()
                    @Suppress("UNCHECKED_CAST")
                    (field.get(mc) as MutableList<IResourcePack>).addAll(listOf(
                            "portal",
                            "transition"
                    ).map { FolderResourcePack(File(root, "src/$it/resources")) })
                }
            } catch (ignored: NoSuchFieldException) {
            }
        }
    }

    companion object {
        @SidedProxy
        lateinit var PROXY: Proxy
    }
    //#endif
}