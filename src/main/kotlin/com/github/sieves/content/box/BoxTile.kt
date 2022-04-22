package com.github.sieves.content.box

import com.github.sieves.Sieves
import com.github.sieves.content.api.ApiTile
import com.github.sieves.content.api.caps.TrackedEnergy
import com.github.sieves.content.api.caps.TrackedInventory
import com.github.sieves.content.battery.BoxContainer
import com.github.sieves.content.link.Links
import com.github.sieves.content.tile.internal.Configuration
import com.github.sieves.registry.Registry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.network.NetworkHooks
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class BoxTile(pos: BlockPos, state: BlockState) :
    ApiTile<BoxTile>(Registry.Tiles.Box, pos, state, "tile.sieves.box"),
    Nameable {
    private val removals = ArrayList<BlockPos>(20)
    override val energy = TrackedEnergy(250_000, ::update)
    private val energyHandler = LazyOptional.of { energy }
    override val items = TrackedInventory(24, ::update)
    private val itemHandler = LazyOptional.of { items }
    private var tick = 0
    val links = Links()
    val powerCost: Int get() = ((links.getLinks().size * 600) / configuration.efficiencyModifier).roundToInt()
    val sleepTime: Int get() = ((20 * 60) / configuration.speedModifier).roundToInt()
    override val ioPower: Int get() = ((links.getLinks().size * 600) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.speedModifier.roundToInt()) * 16) + 1)


    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()

        if (extractPower()) {
            exportItems()
            if (tick >= 20) {
                validateExports()
                update()
                tick = 0
            }
            tick++
        }
    }

    private fun extractPower(): Boolean {
        val extracted = energy.extractEnergy(powerCost, true)
        if (extracted != powerCost) return false
        energy.extractEnergy(powerCost, false)
        return true
    }

    /**
     * Extracts out power to the links
     */
    private fun exportItems() {
        for (link in links.getLinks()) {
            val other = level?.getBlockEntity(link.key) ?: continue
            for (side in link.value) {
                val cap = other.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)
                if (cap.isPresent) {
                    for (slot in 0 until items.slots) {
                        val extracted = items.extractItem(slot, ioRate, true)
                        if (!extracted.isEmpty) {
                            val result = ItemHandlerHelper.insertItem(cap.resolve().get(), extracted, true)
                            if (result.isEmpty) {
                                items.extractItem(slot, extracted.count, false)
                                ItemHandlerHelper.insertItem(cap.resolve().get(), extracted, false)
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Makes sure all the export tiles are valid
     */
    private fun validateExports() {
        removals.clear()
        for (link in links.getLinks()) {
            val tile = level?.getBlockEntity(link.key)
            if (tile == null)
                removals.add(link.key)
            else {
                link.value.forEach {
                    val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, it)
                    if (!cap.isPresent) removals.add(link.key)
                }
            }
        }
        removals.forEach(links::removeLink)
    }


    /**
     * Called when saving nbt data
     */
    override fun onSave(tag: CompoundTag) {
        tag.put("energy", energy.serializeNBT())
        tag.put("items", items.serializeNBT())
        tag.put("links", links.serializeNBT())
    }

    /**
     * Called when loading the nbt data
     */
    override fun onLoad(tag: CompoundTag) {
        energy.deserializeNBT(tag.get("energy"))
        items.deserializeNBT(tag.getCompound("items"))
        links.deserializeNBT(tag.getCompound("links"))
    }

    /**
     * This is used to open up the container menu
     */
    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> BoxContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

    /**
     * Called on capability invalidation
     */
    override fun onInvalidate() {
        energyHandler.invalidate()
        itemHandler.invalidate()
    }

    override fun getName(): Component {
        return TranslatableComponent("container.${Sieves.ModId}.box")
    }

    /**
     * Returns the amount of energy we currently have
     */
    fun getStoredPower(): Int {
        return this.energy.energyStored
    }

    /**
     * Gets the total amount of energy we can store
     */
    fun getTotalPower(): Int {
        return this.energy.maxEnergyStored
    }

    /**
     * Adds our link and syncs with the client
     */
    fun linkTo(other: BlockPos, face: Direction) {
        links.addLink(other, face)
        update()
    }


    /**
     * Removes our link and syncs with the client
     */
    fun unlink() {
        links.removeLinks()
        update()
    }

    override fun getRenderBoundingBox(): AABB {
        return INFINITE_EXTENT_AABB
    }


    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (side == null && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return itemHandler.cast()
        if (cap == CapabilityEnergy.ENERGY && side == null) return energyHandler.cast()
        if (side == null) return super.getCapability(cap, null)
        return when (getConfig()[side]) {
            Configuration.SideConfig.InputItem -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputPower -> if (CapabilityEnergy.ENERGY == cap) return energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.OutputItem -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.OutputPower -> if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.None -> LazyOptional.empty()
            Configuration.SideConfig.InputOutputItems -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputOutputPower -> if (CapabilityEnergy.ENERGY == cap) return energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputOutputAll -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
        }

    }
}