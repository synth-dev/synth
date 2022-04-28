package com.github.sieves.api

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.ApiConfig.SideConfig.*
import com.github.sieves.content.machines.materializer.*
import com.github.sieves.content.machines.synthesizer.*
import com.github.sieves.registry.Registry.Items
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.*
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*
import net.minecraftforge.energy.*
import net.minecraftforge.fluids.capability.*
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.*
import net.minecraftforge.fluids.capability.templates.*
import net.minecraftforge.items.*
import java.util.function.*
import kotlin.collections.HashMap
import kotlin.properties.*
import kotlin.reflect.*
import net.minecraft.world.item.Items as McItems

abstract class ApiTile<T : ApiTile<T>>(
    type: BlockEntityType<T>, pos: BlockPos, state: BlockState, nameKey: String
) : BlockEntity(type, pos, state), Nameable {
    private val reflectedFields = HashMap<String, Pair<INBTSerializable<*>, Boolean>>()
    protected val configuration: ApiConfig = ApiConfig { update() }.identityConfiguration()
    private val name = TranslatableComponent(nameKey)

    fun getConfig(): ApiConfig = configuration

    abstract val energy: EnergyStorage
    protected val energyHandler: LazyOptional<EnergyStorage> = LazyOptional.of { energy }
    abstract val items: ItemStackHandler
    protected val itemHandler: LazyOptional<IItemHandler> = LazyOptional.of { items }
    abstract val tank: FluidTank
    val tankHandler: LazyOptional<IFluidHandler> = LazyOptional.of { tank }

    abstract val ioPower: Int
    abstract val ioRate: Int

    /**
     * Updates the block on the client
     */
    fun update() {
//        requestModelDataUpdate()
        setChanged()
        if (level != null) {
            level!!.sendBlockUpdated(worldPosition, blockState, blockState, 3)
        }
    }


    override fun getName(): Component = name

    fun <T : INBTSerializable<*>> serialize(
        name: String, value: T, compoundTag: Boolean = true
    ): ReadOnlyProperty<Any?, T> {
        reflectedFields[name] = value to compoundTag
        return object : ReadOnlyProperty<Any?, T>, Supplier<T>, () -> T {
            override fun get(): T = reflectedFields[name]!!.first as T

            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

            override fun invoke(): T = get()
        }
    }

    /**
     * Block a given side for the tile config
     */
    open fun isSideValidFor(side: SideConfig, direction: Direction): Boolean {
        return true
    }

    override fun getRenderBoundingBox(): AABB {
        return INFINITE_EXTENT_AABB
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Serialize(val compoundTag: Boolean = true)


    protected fun autoExport() {
        for (key in Direction.values()) {
            val value = getConfig()[key]
            val tile = level?.getBlockEntity(blockPos.offset(key.normal)) ?: continue
            exportPower(value, tile, key)
            exportItems(value, tile, key)
            exportFluids(value, tile, key)
        }
    }

    protected open fun exportPower(value: SideConfig, tile: BlockEntity, direction: Direction) {
        if (value.canExportPower) {
            val cap = tile.getCapability(CapabilityEnergy.ENERGY, direction.opposite)
            if (cap.isPresent) {
                val other = cap.resolve().get()
                val extracted = energy.extractEnergy(ioPower, true)
                val leftOver = other.receiveEnergy(extracted, false)
                energy.extractEnergy(leftOver, false)
            }
        }
    }

    protected open fun exportItems(value: SideConfig, tile: BlockEntity, direction: Direction) {
        if (value.canExportItem) {
            val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.opposite)
            if (cap.isPresent) {
                val other = cap.resolve().get()
                if (this is MaterializerTile) {
                    for (slot in 1 until this.items.slots) {
                        if (items.getStackInSlot(slot).isEmpty) continue
                        val extracted = items.extractItem(slot, ioRate, false)
                        val leftOver = ItemHandlerHelper.insertItem(other, extracted, false)
                        if (leftOver.isEmpty && !extracted.isEmpty) return
                        items.insertItem(slot, leftOver, false)
                    }
                } else if (this is SynthesizerTile) {
                    if (items.getStackInSlot(2).isEmpty) return
                    val extracted = items.extractItem(2, ioRate, false)
                    val leftOver = ItemHandlerHelper.insertItem(other, extracted, false)
                    if (leftOver.isEmpty && !extracted.isEmpty) return
                    items.insertItem(2, leftOver, false)
                } else for (slot in 0 until this.items.slots) {
                    if (items.getStackInSlot(slot).isEmpty) continue
                    val extracted = items.extractItem(slot, ioRate, false)
                    val leftOver = ItemHandlerHelper.insertItem(other, extracted, false)
                    if (leftOver.isEmpty && !extracted.isEmpty) break
                    items.insertItem(slot, leftOver, false)
                }
            }
        }
    }

    protected open fun exportFluids(value: SideConfig, tile: BlockEntity, direction: Direction) {
        if (value.canExportFluid && tank.capacity > 0) {
            val cap = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.opposite)
            if (cap.isPresent) {
                val other = cap.resolve().get()
                val extracted = tank.drain(this.ioRate, SIMULATE)
                if (other.fill(extracted, SIMULATE) == extracted.amount) {
                    other.fill(tank.drain(this.ioRate, EXECUTE), EXECUTE)
                }
            }
        }
    }

    protected open fun importPower(value: SideConfig, tile: BlockEntity, key: Direction) {
        if (value.canImportPower) {
            val cap = tile.getCapability(CapabilityEnergy.ENERGY, key.opposite)
            if (cap.isPresent) {
                val other = cap.resolve().get()
                val extracted = other.extractEnergy(ioPower, true)
                val leftOver = energy.receiveEnergy(extracted, false)
                other.extractEnergy(leftOver, false)
            }
        }
    }

    protected open fun importItems(value: SideConfig, tile: BlockEntity, key: Direction) {
        if (value.canImportItem) {
            val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, key.opposite)
            if (cap.isPresent) {
                val other = cap.resolve().get()
                for (slot in 0 until other.slots) {
                    if (other.getStackInSlot(slot).isEmpty) continue
                    val extracted = other.extractItem(slot, ioRate, false)

                    val leftOver =
                        if (this is SynthesizerTile) if (extracted.`is`(Items.Linker) || extracted.`is`(McItems.ENDER_EYE)
                        ) items.insertItem(
                            1,
                            extracted,
                            false
                        ) else items.insertItem(
                            0,
                            extracted,
                            false
                        ) else if (this is MaterializerTile) items.insertItem(
                            0,
                            extracted,
                            false
                        ) else ItemHandlerHelper.insertItem(items, extracted, false)
                    if (leftOver.isEmpty && !extracted.isEmpty) break
                    other.insertItem(slot, leftOver, false)
                }
            }
        }
    }

    protected open fun importFluids(value: SideConfig, tile: BlockEntity, key: Direction) {
        if (value.canImportFluid && tank.capacity > 0) {
            val cap = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, key.opposite)
            if (cap.isPresent) {
                val other = cap.resolve().get()
                val extracted = other.drain(this.ioRate, SIMULATE)
                val result = tank.fill(extracted, SIMULATE)
                if (tank.fill(extracted, SIMULATE) == extracted.amount) {
                    tank.fill(other.drain(this.ioRate, EXECUTE), EXECUTE)
                }
            }
        }
    }

    protected fun autoImport() {
        for (key in Direction.values()) {
            val value = getConfig()[key]
            val tile = level?.getBlockEntity(blockPos.offset(key.normal)) ?: continue
            importPower(value, tile, key)
            importItems(value, tile, key)
            importFluids(value, tile, key)
        }
    }

    /**
     * This gets the direction based upon the relative side
     */
    open fun getRelative(side: ApiConfig.Side): Direction {
        val front = blockState.getValue(DirectionalBlock.FACING)
        return when (side) {
            ApiConfig.Side.Top -> {
                if (front == Direction.UP) return Direction.NORTH
                else if (front == Direction.DOWN) return Direction.SOUTH
                Direction.UP
            }
            ApiConfig.Side.Bottom -> {
                if (front == Direction.UP) return Direction.SOUTH
                else if (front == Direction.DOWN) return Direction.NORTH
                Direction.DOWN
            }
            ApiConfig.Side.Front -> {
                front.opposite
            }
            ApiConfig.Side.Back -> {
                front
            }
            ApiConfig.Side.Left -> {
                if (front == Direction.UP) return Direction.WEST
                else if (front == Direction.DOWN) return Direction.EAST
                front.opposite.counterClockWise
            }
            ApiConfig.Side.Right -> {
                if (front == Direction.UP) return Direction.EAST
                else if (front == Direction.DOWN) return Direction.WEST
                front.opposite.clockWise
            }
        }
    }

    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (side == null && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && items.slots > 0) return itemHandler.cast()
        if (side == null && cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && tank.capacity > 0) return tankHandler.cast()
        if (side == null && cap == CapabilityEnergy.ENERGY && energy.maxEnergyStored > 0) return energyHandler.cast()
        if (side == null) return super.getCapability(cap, null)
        return when (getConfig()[side]) {
            InputItem, OutputItem, InputOutputItems -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) itemHandler.cast() else LazyOptional.empty()
            InputPower, OutputPower, InputOutputPower -> if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
            InputFluid, OutputFluid, InputOutputFluid -> if (CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY == cap) tankHandler.cast() else LazyOptional.empty()
            InputOutputAll -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) itemHandler.cast()
            else if (CapabilityEnergy.ENERGY == cap) energyHandler.cast()
            else if (CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY == cap) tankHandler.cast()
            else LazyOptional.empty()
            None -> LazyOptional.empty()
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
    open fun onMenu(player: ServerPlayer) {
    }

    /**
     * Called on capability invalidation
     */
    protected open fun onInvalidate() {
        this.energyHandler.invalidate()
        this.tankHandler.invalidate()
        this.itemHandler.invalidate()
    }

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
        extra.put("items", items.serializeNBT())
        extra.put("energy", energy.serializeNBT())
        extra.put(
            "fluids", this.tank.writeToNBT(CompoundTag())
        )
        tag.put("extra_data", extra)
    }

    @Suppress("UNCHECKED_CAST")
    override fun load(tag: CompoundTag) {
        super.load(tag)
        onLoad(tag)
        val extra = tag.getCompound("extra_data")
        configuration.deserializeNBT(extra.getCompound("configuration"))
        items.deserializeNBT(extra.getCompound("items"))
        energy.deserializeNBT(extra.get("energy"))
        tank.readFromNBT(extra.getCompound("fluids"))
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