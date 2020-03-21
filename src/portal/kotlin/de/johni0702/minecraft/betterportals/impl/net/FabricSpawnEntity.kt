//#if FABRIC>=1
//$$ package de.johni0702.minecraft.betterportals.impl.net
//$$
//$$ import de.johni0702.minecraft.betterportals.impl.IMessage
//$$ import de.johni0702.minecraft.betterportals.impl.IMessageHandler
//$$ import de.johni0702.minecraft.betterportals.impl.MessageContext
//$$ import de.johni0702.minecraft.betterportals.impl.NetworkDirection
//$$ import de.johni0702.minecraft.betterportals.impl.sync
//$$ import io.netty.buffer.ByteBuf
//$$ import net.minecraft.client.MinecraftClient
//$$ import net.minecraft.client.network.packet.EntityS2CPacket
//$$ import net.minecraft.client.network.packet.EntitySpawnS2CPacket
//$$ import net.minecraft.util.PacketByteBuf
//$$
//$$ internal class FabricSpawnEntity(private var inner: EntitySpawnS2CPacket = EntitySpawnS2CPacket()) : IMessage {
//$$     override val direction = NetworkDirection.TO_CLIENT
//$$
//$$     override fun fromBytes(buf: ByteBuf) {
//$$         inner.read(PacketByteBuf(buf))
//$$     }
//$$
//$$     override fun toBytes(buf: ByteBuf) {
//$$         inner.write(PacketByteBuf(buf))
//$$     }
//$$
//$$     internal class Handler : IMessageHandler<FabricSpawnEntity> {
//$$         override fun new(): FabricSpawnEntity = FabricSpawnEntity()
//$$
//$$         override fun handle(message: FabricSpawnEntity, ctx: MessageContext) {
//$$             ctx.sync {
//$$                 val world = MinecraftClient.getInstance().world
//$$                 with(message.inner) {
//$$                     val entity = entityTypeId.create(world) ?: return@sync
//$$                     entity.updateTrackedPosition(x, y, z);
//$$                     entity.setVelocity(velocityX, velocityY, velocityz);
//$$                     entity.pitch = (pitch * 360) / 256.0F;
//$$                     entity.yaw = (yaw * 360) / 256.0F;
//$$                     entity.entityId = id;
//$$                     entity.uuid = uuid;
//$$                     val pos = EntityS2CPacket.decodePacketCoordinates(entity.trackedX, entity.trackedY, entity.trackedZ)
//$$                     entity.setPositionAndAngles(pos.x, pos.y, pos.z, entity.yaw, entity.pitch)
//$$                     world.addEntity(id, entity);
//$$                 }
//$$             }
//$$         }
//$$     }
//$$ }
//#endif