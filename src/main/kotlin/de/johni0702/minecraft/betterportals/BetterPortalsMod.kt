package de.johni0702.minecraft.betterportals

import com.google.common.util.concurrent.ListenableFutureTask
import de.johni0702.minecraft.betterportals.client.renderer.RenderEndPortal
import de.johni0702.minecraft.betterportals.client.renderer.RenderNetherPortal
import de.johni0702.minecraft.betterportals.client.view.ClientViewManager
import de.johni0702.minecraft.betterportals.client.view.ClientViewManagerImpl
import de.johni0702.minecraft.betterportals.client.view.ViewDemuxingTaskQueue
import de.johni0702.minecraft.betterportals.common.blocks.BlockBetterEndPortal
import de.johni0702.minecraft.betterportals.common.blocks.BlockBetterNetherPortal
import de.johni0702.minecraft.betterportals.common.capability.*
import de.johni0702.minecraft.betterportals.common.entity.EndEntryPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.EndExitPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.NetherPortalEntity
import de.johni0702.minecraft.betterportals.common.logFailure
import de.johni0702.minecraft.betterportals.net.Net
import de.johni0702.minecraft.betterportals.server.view.AttachServerViewManagerCapability
import de.johni0702.minecraft.betterportals.server.view.ServerViewManager
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.registry.EntityRegistry
import org.apache.logging.log4j.Logger

const val MOD_ID = "betterportals"

lateinit var LOGGER: Logger

@Mod(modid = MOD_ID, useMetadata = true)
class BetterPortalsMod {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        INSTANCE = this
        LOGGER = event.modLog
        PROXY.preInit(this)

        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun registerBlocks(event: RegistryEvent.Register<Block>) {
        with(event.registry) {
            register(BlockBetterNetherPortal())
            register(BlockBetterEndPortal())
        }
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        PROXY.init(this)
    }

    interface Proxy {
        fun preInit(mod: BetterPortalsMod)
        fun init(mod: BetterPortalsMod)
        fun sync(world: World?, runnable: () -> Unit)
        fun nextTick(world: World?, runnable: () -> Unit)
    }

    internal abstract class CommonProxy : Proxy {
        override fun preInit(mod: BetterPortalsMod) {}

        override fun init(mod: BetterPortalsMod) {
            Net.INSTANCE // initialize via <init>

            CapabilityManager.INSTANCE.register(ServerViewManager::class.java, NoStorage(), { throw UnsupportedOperationException() })
            MinecraftForge.EVENT_BUS.register(AttachServerViewManagerCapability())

            // Tickets are only allocated temporarily during remote portal frame search and otherwise aren't needed
            ForgeChunkManager.setForcedChunkLoadingCallback(mod, { tickets, _ ->
                tickets.forEach { ForgeChunkManager.releaseTicket(it) }
            })

            EntityRegistry.registerModEntity(
                    ResourceLocation(MOD_ID, "nether_portal"),
                    NetherPortalEntity::class.java,
                    "nether_portal",
                    0,
                    mod,
                    256,
                    Int.MAX_VALUE,
                    false
            )
            EntityRegistry.registerModEntity(
                    ResourceLocation(MOD_ID, "end_entry_portal"),
                    EndEntryPortalEntity::class.java,
                    "end_entry_portal",
                    1,
                    mod,
                    256,
                    Int.MAX_VALUE,
                    false
            )
            EntityRegistry.registerModEntity(
                    ResourceLocation(MOD_ID, "end_exit_portal"),
                    EndExitPortalEntity::class.java,
                    "end_exit_portal",
                    2,
                    mod,
                    256,
                    Int.MAX_VALUE,
                    false
            )
        }

        override fun sync(world: World?, runnable: () -> Unit) {
            world!!.minecraftServer!!.addScheduledTask(runnable).logFailure()
        }

        override fun nextTick(world: World?, runnable: () -> Unit) {
            TODO("not implemented")
        }
    }

    @Suppress("unused")
    internal class ServerProxy : CommonProxy()

    @Suppress("unused")
    internal class ClientProxy : CommonProxy() {
        override fun preInit(mod: BetterPortalsMod) {
            RenderingRegistry.registerEntityRenderingHandler(NetherPortalEntity::class.java, ::RenderNetherPortal)
            RenderingRegistry.registerEntityRenderingHandler(EndEntryPortalEntity::class.java, ::RenderEndPortal)
            RenderingRegistry.registerEntityRenderingHandler(EndExitPortalEntity::class.java, ::RenderEndPortal)
        }

        override fun init(mod: BetterPortalsMod) {
            super.init(mod)
            viewManagerImpl.init()

            val mc = Minecraft.getMinecraft()
            synchronized(mc.scheduledTasks) {
                mc.scheduledTasks = ViewDemuxingTaskQueue(mc, mc.scheduledTasks)
            }
        }

        override fun sync(world: World?, runnable: () -> Unit) {
            if (world is WorldServer) {
                super.sync(world, runnable)
            } else if (world == null || world is WorldClient) {
                Minecraft.getMinecraft().addScheduledTask(runnable).logFailure()
            } else {
                throw UnsupportedOperationException("Cannot determine side.")
            }
        }

        /**
         * Set when the currently running code has been scheduled by runLater.
         * If this is the case, subsequent calls to runLater have to be delayed until all scheduled tasks have been
         * processed, otherwise a live-lock may occur.
         */
        private var inRunLater = false

        override fun nextTick(world: World?, runnable: () -> Unit) {
            if (world is WorldServer) {
                super.nextTick(world, runnable)
            } else if (world == null || world is WorldClient) {
                val mc = Minecraft.getMinecraft()
                if (mc.isCallingFromMinecraftThread && inRunLater) {
                    val bus = MinecraftForge.EVENT_BUS
                    bus.register(object : Any() {
                        @SubscribeEvent
                        fun onRenderTick(event: TickEvent.RenderTickEvent) {
                            if (event.phase == TickEvent.Phase.START) {
                                nextTick(world, runnable)
                                bus.unregister(this)
                            }
                        }
                    })
                    return
                }
                val queue = mc.scheduledTasks
                synchronized(queue) {
                    queue.add(ListenableFutureTask.create({
                        inRunLater = true
                        try {
                            runnable()
                        } finally {
                            inRunLater = false
                        }
                    }, null).logFailure())
                }
            } else {
                throw UnsupportedOperationException("Cannot determine side.")
            }
        }
    }

    companion object {
        @SidedProxy
        lateinit var PROXY: Proxy
        lateinit var INSTANCE: BetterPortalsMod

        internal val viewManagerImpl by lazy { ClientViewManagerImpl() }
        val viewManager: ClientViewManager by lazy { viewManagerImpl }
    }
}
