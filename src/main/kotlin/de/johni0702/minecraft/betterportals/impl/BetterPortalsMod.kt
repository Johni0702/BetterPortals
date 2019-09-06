package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.BetterPortalsAPI
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.impl.common.initPortal
import de.johni0702.minecraft.betterportals.impl.transition.common.initTransition
import de.johni0702.minecraft.betterportals.impl.vanilla.common.initVanilla
import de.johni0702.minecraft.view.common.ViewAPI
import de.johni0702.minecraft.view.impl.ViewAPIImpl
import de.johni0702.minecraft.view.impl.common.initView
import net.minecraft.block.Block
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.registries.IForgeRegistry
import org.apache.logging.log4j.Logger

//#if MC>=11400
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraft.tileentity.TileEntityType
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
//$$ import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
//#else
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.initAbyssalcraft
import de.johni0702.minecraft.betterportals.impl.aether.common.initAether
import de.johni0702.minecraft.betterportals.impl.mekanism.common.initMekanism
import de.johni0702.minecraft.betterportals.impl.tf.common.initTwilightForest
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.initTravelHuts
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.FolderResourcePack
import net.minecraft.client.resources.IResourcePack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import java.io.File
//#endif

const val MOD_ID = "betterportals"

lateinit var LOGGER: Logger

//#if MC>=11400
//$$ @Mod(MOD_ID)
//#else
@Mod(modid = MOD_ID, useMetadata = true)
//#endif
internal class BetterPortalsMod: ViewAPI by ViewAPIImpl, BetterPortalsAPI by BetterPortalsAPIImpl {

    internal val clientPreInitCallbacks = mutableListOf<() -> Unit>()
    internal val commonInitCallbacks = mutableListOf<() -> Unit>()
    internal val clientInitCallbacks = mutableListOf<() -> Unit>()
    internal val commonPostInitCallbacks = mutableListOf<() -> Unit>()
    internal val clientPostInitCallbacks = mutableListOf<() -> Unit>()
    private val registerBlockCallbacks = mutableListOf<IForgeRegistry<Block>.() -> Unit>()
    private val registerTileEntitiesCallbacks = mutableListOf<TileEntityTypeRegistry.() -> Unit>()
    private val registerEntitiesCallbacks = mutableListOf<EntityTypeRegistry.() -> Unit>()

    init {
        INSTANCE = this
    //#if MC>=11400
    //$$ }
    //$$
    //$$ @SubscribeEvent(priority = EventPriority.HIGH)
    //$$ fun init(event: FMLCommonSetupEvent) {
    //$$     BPConfig.load()
    //#endif

        fun PortalConfig.toConfiguration() = PortalConfiguration(
                { opacity },
                { renderDistMin },
                { renderDistMax },
                { renderDistSizeMultiplier }
        )

        initView(
                init = { commonInitCallbacks.add(it) },
                clientInit = { clientInitCallbacks.add(it) },
                debugView = { BPConfig.debugView }
        )

        initTransition(
                init = { commonInitCallbacks.add(it) },
                enable = BPConfig.enhanceThirdPartyTransfers,
                duration = { BPConfig.enhancedThirdPartyTransferSeconds }
        )

        initPortal(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientInit = { clientInitCallbacks.add(it) },
                preventFallDamage = { BPConfig.preventFallDamage },
                dropRemoteSound = { !BPConfig.soundThroughPortals },
                maxRenderRecursion = { if (BPConfig.seeThroughPortals) BPConfig.recursionLimit else 0 }
        )

        initVanilla(
                mod = this,
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                registerTileEntities = { registerTileEntitiesCallbacks.add(it) },
                registerEntities = { registerEntitiesCallbacks.add(it) },
                enableNetherPortals = BPConfig.netherPortals.enabled,
                enableEndPortals = BPConfig.endPortals.enabled,
                configNetherPortals = BPConfig.netherPortals.toConfiguration(),
                configEndPortals = BPConfig.endPortals.toConfiguration()
        )

        //#if MC<11400
        initTwilightForest(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                enableTwilightForestPortals = BPConfig.twilightForestPortals.enabled,
                configTwilightForestPortals = BPConfig.twilightForestPortals.toConfiguration()
        )

        initMekanism(
                init = { commonInitCallbacks.add(it) },
                postInit = { commonPostInitCallbacks.add(it) },
                clientPostInit = { clientPostInitCallbacks.add(it) },
                enableMekanismPortals = BPConfig.mekanismPortals.enabled,
                configMekanismPortals = BPConfig.mekanismPortals.toConfiguration()
        )

        initAether(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                enableAetherPortals = BPConfig.aetherPortals.enabled,
                configAetherPortals = BPConfig.aetherPortals.toConfiguration()
        )

        initAbyssalcraft(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                enableAbyssalcraftPortals = BPConfig.abyssalcraftPortals.enabled,
                configAbyssalcraftPortals = BPConfig.abyssalcraftPortals.toConfiguration()
        )

        initTravelHuts(
                mod = this,
                init = { commonInitCallbacks.add(it) },
                clientPreInit = { clientPreInitCallbacks.add(it) },
                registerBlocks = { registerBlockCallbacks.add(it) },
                enableTravelHutsPortals = BPConfig.travelHutsPortals.enabled,
                configTravelHutsPortals = BPConfig.travelHutsPortals.toConfiguration()
        )
        //#endif
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun registerBlocks(event: RegistryEvent.Register<Block>) {
        with(event.registry) {
            registerBlockCallbacks.forEach { it() }
        }
    }

    //#if MC>=11400
    //$$ @SubscribeEvent(priority = EventPriority.LOW)
    //$$ fun registerTileEntities(event: RegistryEvent.Register<TileEntityType<*>>) {
    //$$     with(event.registry) {
    //$$         registerTileEntitiesCallbacks.forEach { it() }
    //$$     }
    //$$ }
    //$$
    //$$ @SubscribeEvent(priority = EventPriority.LOW)
    //$$ fun registerEntities(event: RegistryEvent.Register<EntityType<*>>) {
    //$$     with(event.registry) {
    //$$         registerEntitiesCallbacks.forEach { it() }
    //$$     }
    //$$ }
    //#endif

    //#if MC>=11400
    //$$ @SubscribeEvent
    //$$ fun commonInit(event: FMLCommonSetupEvent) {
    //$$     commonInitCallbacks.forEach { it() }
    //$$ }
    //$$
    //$$ @SubscribeEvent
    //$$ fun clientInit(event: FMLClientSetupEvent) {
    //$$     clientPreInitCallbacks.forEach { it() }
    //$$     clientInitCallbacks.forEach { it() }
    //$$ }
    //#else
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        LOGGER = event.modLog
        PROXY.preInit(this)

        MinecraftForge.EVENT_BUS.register(this)
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        PROXY.init(this)
    }

    @Mod.EventHandler
    fun preInit(event: FMLPostInitializationEvent) {
        PROXY.postInit(this)
    }

    interface Proxy {
        fun preInit(mod: BetterPortalsMod)
        fun init(mod: BetterPortalsMod)
        fun postInit(mod: BetterPortalsMod)
    }

    internal abstract class CommonProxy : Proxy {
        override fun preInit(mod: BetterPortalsMod) {}

        override fun init(mod: BetterPortalsMod) {
            //#if MC<11400
            BetterPortalsMod.INSTANCE.registerTileEntitiesCallbacks.forEach { with(TileEntityTypeRegistry) { it() } }
            BetterPortalsMod.INSTANCE.registerEntitiesCallbacks.forEach { with(EntityTypeRegistry) { it() } }
            //#endif
            BetterPortalsMod.INSTANCE.commonInitCallbacks.forEach { it() }
        }

        override fun postInit(mod: BetterPortalsMod) {
            BetterPortalsMod.INSTANCE.commonPostInitCallbacks.forEach { it() }
        }
    }

    @Suppress("unused")
    internal class ServerProxy : CommonProxy()

    @Suppress("unused")
    internal class ClientProxy : CommonProxy() {
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

        override fun preInit(mod: BetterPortalsMod) {
            BetterPortalsMod.INSTANCE.clientPreInitCallbacks.forEach { it() }
        }

        override fun init(mod: BetterPortalsMod) {
            BetterPortalsMod.INSTANCE.clientInitCallbacks.forEach { it() }
            super.init(mod)
        }

        override fun postInit(mod: BetterPortalsMod) {
            super.postInit(mod)
            BetterPortalsMod.INSTANCE.clientPostInitCallbacks.forEach { it() }
        }
    }
    //#endif

    companion object {
        //#if MC<11400
        @SidedProxy
        lateinit var PROXY: Proxy
        //#endif
        lateinit var INSTANCE: BetterPortalsMod
    }
}