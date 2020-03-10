package de.johni0702.minecraft.betterportals.impl

//#if FABRIC>=1
//$$ import de.johni0702.minecraft.view.common.viewApiImpl
//$$ import net.minecraft.util.registry.Registry
//$$ import org.apache.logging.log4j.LogManager
//#else
import net.minecraft.block.Block
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
//#endif

//#if MC>=11400
//#if FABRIC>=1
//$$ import net.fabricmc.api.ClientModInitializer;
//$$ import net.fabricmc.api.ModInitializer;
//#else
//$$ import net.alexwells.kottle.FMLKotlinModLoadingContext
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraft.tileentity.TileEntityType
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
//$$ import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
//#endif
//#else
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.FMLModContainer
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.relauncher.Side
//#endif

abstract class ModBase
//#if FABRIC>=1
//$$     : ModInitializer, ClientModInitializer
//#endif
{
    open val canLoad = true

    open fun BlockRegistry.registerBlocks() = Unit
    open fun TileEntityTypeRegistry.registerBlockEntities() = Unit
    open fun EntityTypeRegistry.registerEntities() = Unit

    open fun commonPreInit() = Unit
    open fun commonInit() = Unit
    open fun commonPostInit() = Unit

    open fun clientPreInit() = Unit
    open fun clientInit() = Unit
    open fun clientPostInit() = Unit

    //#if FABRIC>=1
    //$$ override fun onInitialize() {
    //$$     if (!canLoad) return
    //$$     with(Registry.BLOCK) { registerBlocks() }
    //$$     with(Registry.BLOCK_ENTITY) { registerBlockEntities() }
    //$$     with(Registry.ENTITY_TYPE) { registerEntities() }
    //$$     commonPreInit()
    //$$     commonInit()
    //$$     commonPostInit()
    //$$ }
    //$$
    //$$ override fun onInitializeClient() {
    //$$     if (!canLoad) return
    //$$     clientPreInit()
    //$$     clientInit()
    //$$     clientPostInit()
    //$$ }
    //#else
    @SubscribeEvent(priority = EventPriority.LOW)
    fun forgeRegisterBlocksEvent(event: RegistryEvent.Register<Block>) {
        with(event.registry) {
            registerBlocks()
        }
    }

    //#if MC>=11400
    //$$ init { if (canLoad) FMLKotlinModLoadingContext.get().modEventBus.register(this) }
    //$$
    //$$ @SubscribeEvent(priority = EventPriority.LOW)
    //$$ fun forgeRegisterTileEntitiesEvent(event: RegistryEvent.Register<TileEntityType<*>>) {
    //$$     with(event.registry) {
    //$$         registerBlockEntities()
    //$$     }
    //$$ }
    //$$
    //$$ @SubscribeEvent(priority = EventPriority.LOW)
    //$$ fun forgeRegisterEntitiesEvent(event: RegistryEvent.Register<EntityType<*>>) {
    //$$     with(event.registry) {
    //$$         registerEntities()
    //$$     }
    //$$ }
    //$$
    //$$ @SubscribeEvent
    //$$ fun forgeCommonInit(event: FMLCommonSetupEvent) {
    //$$     commonPreInit()
    //$$     commonInit()
    //$$     commonPostInit()
    //$$ }
    //$$
    //$$ @SubscribeEvent
    //$$ fun forgeClientInit(event: FMLClientSetupEvent) {
    //$$     clientPreInit()
    //$$     clientInit()
    //$$     clientPostInit()
    //$$ }
    //#else
    init {
        // Forge doesn't check subclasses for Mod.EventHandler annotation which sucks,
        // and 1.12.2 is technically no longer supported, so fuck Forge, we'll just do it ourselves
        val container = Loader.instance().activeModContainer()!!
        val method = FMLModContainer::class.java.getDeclaredMethod("gatherAnnotations", Class::class.java)
        method.isAccessible = true
        method.invoke(container, ModBase::class.java)
    }

    @Mod.EventHandler
    fun forgePreInit(event: FMLPreInitializationEvent) {
        if (!canLoad) return
        MinecraftForge.EVENT_BUS.register(this)

        commonPreInit()
        if (event.side == Side.CLIENT) {
            clientPreInit()
        }
    }

    @Mod.EventHandler
    fun forgeInit(event: FMLInitializationEvent) {
        if (!canLoad) return
        with(TileEntityTypeRegistry) { registerBlockEntities() }
        with(EntityTypeRegistry) { registerEntities() }
        commonInit()
        if (event.side == Side.CLIENT) {
            clientInit()
        }
    }

    @Mod.EventHandler
    fun forgePostInit(event: FMLPostInitializationEvent) {
        if (!canLoad) return
        commonPostInit()
        if (event.side == Side.CLIENT) {
            clientPostInit()
        }
    }
    //#endif
    //#endif
}