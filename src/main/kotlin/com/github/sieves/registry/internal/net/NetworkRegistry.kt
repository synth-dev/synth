package com.github.sieves.registry.internal.net

import com.github.sieves.registry.internal.IRegister
import com.github.sieves.util.Log.debug
import com.github.sieves.util.Log.info
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel
import net.minecraftforge.server.ServerLifecycleHooks
import java.util.UUID
import java.util.function.BiConsumer
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


abstract class NetworkRegistry : IRegister {
    private lateinit var instance: SimpleChannel
    protected val listeners: MutableMap<Class<out Packet>, PacketWrapper<Packet>> = HashMap()

    @Suppress("UNCHECKED_CAST")
    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        val name = "${modId}_network"
        debug { "Registering bmp network instance with name: $name" }
        modBus.addListener { _: FMLCommonSetupEvent ->
            instance = NetworkRegistry.newSimpleChannel(
                ResourceLocation(modId, name),
                { "1.0" },
                { true },
                { true }
            )
            info { "Registering packets for network '$name'..." }
            for (packet in listeners) {
                info { "registering packet class ${packet.key.name} to ${packet.value.id}" }
                val packetClass = packet.key
                val listener = packet.value
                instance.messageBuilder(packetClass as Class<Packet>, packet.value.id)
                    .encoder { pkt, buffer ->
                        pkt.write(buffer)
                    }
                    .decoder {
                        val newPacket = listener()
                        newPacket.read(it)
                        newPacket
                    }
                    .consumer(BiConsumer { pkt, ctx ->
                        listener.invoke(pkt, ctx.get())
                    })
                    .add()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified T : Packet> register(
        id: Int,
        noinline packetSupplier: () -> T
    ): ReadOnlyProperty<Any?, PacketWrapper<T>> {
        val packetListener = PacketWrapper(id, packetSupplier)
        listeners[T::class.java] = packetListener as PacketWrapper<Packet>
        return object : ReadOnlyProperty<Any?, PacketWrapper<T>>, Supplier<PacketWrapper<T>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) = packetListener
            override fun get() = packetListener
        }
    }


    /**
     * This will broadcast to all clients.
     *
     * @param packet the packet to broadcast
     */
    fun sendToAllClients(
        packet: Packet
    ) = instance.send<Any>(PacketDistributor.ALL.noArg(), packet)

    /**
     * This will broadcast to all the clients with the specified chunk.
     *
     * @param packet the packet to send
     * @param chunk  the chunk to use
     */
    fun sendToClientsWithChunk(packet: Packet, chunk: LevelChunk) =
        instance.send<Any>(PacketDistributor.TRACKING_CHUNK.with { chunk }, packet)


    /**
     * This will broadcast to all the clients with the specified chunk.
     *
     * @param packet the packet to send
     * @param near   The target point to use as reference for what is near
     */
    fun sendToClientsWithBlockLoaded(
        packet: Packet,
        blockPos: BlockPos,
        world: Level
    ) = instance.send<Any>(PacketDistributor.TRACKING_CHUNK.with { world.getChunkAt(blockPos) }, packet)

    /**
     * This will broadcast to all the clients with the specified chunk.
     *
     * @param packet the packet to send
     * @param near   The target point to use as reference for what is near
     */
    fun sendToClientsWithTileLoaded(
        packet: Packet,
        tile: BlockEntity,
    ) = instance.send<Any>(PacketDistributor.TRACKING_CHUNK.with { tile.level?.getChunkAt(tile.blockPos) }, packet)


    /**
     * This will send the packet directly to the server
     *
     * @param packet the packet to be sent
     */
    fun <T : Packet> sendToServer(packet: T) =
        instance.sendToServer(packet)

    /**
     * This will send the packet directly to the given player
     *
     * @param packet the packet to send to the client
     * @param player the player to recieve the packet
     */
    fun sendToClient(packet: Packet, player: ServerPlayer) {
        instance.sendTo<Any>(
            packet,
            player.connection.connection,
            NetworkDirection.PLAY_TO_CLIENT
        )
    }

    /**
     * This will send a packet to the player by gettin the instance from the
     * [ServerLifecycleHooks#getCurrentServer] method
     *
     * @param packet the packet to send to the client
     * @param player the player to recieve the packet
     */
    fun sendToClient(packet: Packet, uuid: UUID) {
        sendToClient(
            packet,
            ServerLifecycleHooks.getCurrentServer().playerList.getPlayer(uuid)
                ?: error("Failed to find client with uuid: $uuid ????? (maybe cuz randomly set in packet?)")
        )
    }


    /**
     * This will send the packet directly to the given player
     *
     * @param packet the packet to send to the client
     * @param player the player to recieve the packet
     */
    fun NetworkEvent.Context.sendToClient(packet: Packet) {
        instance.sendTo<Any>(
            packet,
            this.networkManager,
            NetworkDirection.PLAY_TO_CLIENT
        )
    }
}