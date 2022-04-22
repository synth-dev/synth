package com.github.sieves.content.api

import com.github.sieves.content.tile.internal.Configuration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Nameable
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.EnergyStorage
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.items.ItemStackHandler
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class ApiTile<T : ApiTile<T>>(
    type: BlockEntityType<T>,
    pos: BlockPos,
    state: BlockState,
    nameKey: String
) :
    BlockEntity(type, pos, state), Nameable {
    private val reflectedFields = HashMap<String, Pair<INBTSerializable<*>, Boolean>>()
    protected val configuration: Configuration = Configuration { update() }.identityConfiguration()

    fun getConfig(): Configuration = configuration

    abstract val energy: EnergyStorage
    abstract val items: ItemStackHandler
    abstract val ioPower: Int
    abstract val ioRate: Int

    /**
     * Updates the block on the client
     */
    fun update() {
        requestModelDataUpdate()
        setChanged()
        if (level != null) {
            level!!.setBlockAndUpdate(worldPosition, blockState)
            level!!.sendBlockUpdated(worldPosition, blockState, blockState, 3)
        }
    }

    protected fun <T : INBTSerializable<*>> serialize(
        name: String,
        value: T,
        compoundTag: Boolean = true
    ): ReadOnlyProperty<Any?, T> {
        reflectedFields[name] = value to compoundTag
        return object : ReadOnlyProperty<Any?, T>, Supplier<T>, () -> T {
            override fun get(): T = reflectedFields[name]!!.first as T

            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

            override fun invoke(): T = get()
        }
    }


    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Serialize(val compoundTag: Boolean = true)


    protected fun autoExport() {
        for (key in Direction.values()) {
            val value = getConfig()[key]
            val tile = level?.getBlockEntity(blockPos.offset(key.normal)) ?: continue
            if (value.canExportPower) {
                val cap = tile.getCapability(CapabilityEnergy.ENERGY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    val extracted = energy.extractEnergy(ioPower, true)
                    val leftOver = other.receiveEnergy(extracted, false)
                    energy.extractEnergy(leftOver, false)
                }
            } else if (value.canExportItem) {
                val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    for (slot in 0 until this.items.slots) {
                        if (items.getStackInSlot(slot).isEmpty) continue
                        val extracted = items.extractItem(slot, ioRate, false)
                        val leftOver = ItemHandlerHelper.insertItem(other, extracted, false)
                        if (leftOver.isEmpty && !extracted.isEmpty) break
                        items.insertItem(slot, leftOver, false)
                    }
                }
            }
        }
    }

    protected fun autoImport() {
        for (key in Direction.values()) {
            val value = getConfig()[key]
            val tile = level?.getBlockEntity(blockPos.offset(key.normal)) ?: continue
            if (value.canImportPower) {
                val cap = tile.getCapability(CapabilityEnergy.ENERGY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    val extracted = other.extractEnergy(ioPower, true)
                    val leftOver = energy.receiveEnergy(extracted, false)
                    other.extractEnergy(leftOver, false)
                }
            } else if (value.canImportItem) {
                val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    for (slot in 0 until other.slots) {
                        if (other.getStackInSlot(slot).isEmpty) continue
                        val extracted = other.extractItem(slot, ioRate, false)
                        val leftOver = ItemHandlerHelper.insertItem(items, extracted, false)
                        if (leftOver.isEmpty && !extracted.isEmpty) break
                        other.insertItem(slot, leftOver, false)
                    }
                }
            }
        }
    }

    /**
     * This gets the direction based upon the relative side
     */
    open fun getRelative(side: Configuration.Side): Direction {
        val front = blockState.getValue(DirectionalBlock.FACING)
        return when (side) {
            Configuration.Side.Top -> {
                if (front == Direction.UP)
                    return Direction.NORTH
                else if (front == Direction.DOWN)
                    return Direction.SOUTH
                Direction.UP
            }
            Configuration.Side.Bottom -> {
                if (front == Direction.UP)
                    return Direction.SOUTH
                else if (front == Direction.DOWN)
                    return Direction.NORTH
                Direction.DOWN
            }
            Configuration.Side.Front -> {
                front.opposite
            }
            Configuration.Side.Back -> {
                front
            }
            Configuration.Side.Left -> {
                if (front == Direction.UP)
                    return Direction.WEST
                else if (front == Direction.DOWN)
                    return Direction.EAST
                front.opposite.counterClockWise
            }
            Configuration.Side.Right -> {
                if (front == Direction.UP)
                    return Direction.EAST
                else if (front == Direction.DOWN)
                    return Direction.WEST
                front.opposite.clockWise
            }
        }
    }


    /**
     * Called on the client when ticking happens
     */
    protected open fun onClientTick() {}

    /**
     * Called on the server when ticking happens
     */
    protected open fun onServerTick() {}

    /**
     * Called when saving nbt data
     */
    protected open fun onSave(tag: CompoundTag) {}

    /**
     * Called when loading the nbt data
     */
    protected open fun onLoad(tag: CompoundTag) {}

    /**
     * This is used to open up the container menu
     */
    open fun onMenu(player: ServerPlayer) {}

    /**
     * Called on capability invalidation
     */
    protected open fun onInvalidate() {}

    /**
     * Ticks the client and server respectively
     */
    class Ticker<T : ApiTile<T>> : BlockEntityTicker<T> {
        override fun tick(pLevel: Level, pPos: BlockPos, pState: BlockState, pBlockEntity: T) {
            if (pLevel.isClientSide) pBlockEntity.onClientTick()
            else pBlockEntity.onServerTick()
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        onSave(tag)
        val extra = CompoundTag()
        extra.put("configuration", configuration.serializeNBT())
        tag.put("extra_data", extra)
    }

    @Suppress("UNCHECKED_CAST")
    override fun load(tag: CompoundTag) {
        super.load(tag)
        onLoad(tag)
        val extra = tag.getCompound("extra_data")
        configuration.deserializeNBT(extra.getCompound("configuration"))
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(): CompoundTag {
        return serializeNBT()
    }


    override fun invalidateCaps() {
        super.invalidateCaps()
        onInvalidate()
    }

    override fun handleUpdateTag(tag: CompoundTag) {
        super.handleUpdateTag(tag)
        load(tag)
    }

    override fun onDataPacket(net: Connection, pkt: ClientboundBlockEntityDataPacket) {
        super.onDataPacket(net, pkt)
        handleUpdateTag(pkt.tag!!)
    }

}