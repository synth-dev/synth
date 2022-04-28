package com.github.sieves.content.io.battery

import com.github.sieves.Sieves
import com.github.sieves.api.*
import com.github.sieves.api.caps.*
import com.github.sieves.content.io.fluids.*
import com.github.sieves.content.io.link.Links
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
import net.minecraftforge.fluids.capability.*
import net.minecraftforge.fluids.capability.templates.FluidTank
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.network.NetworkHooks
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class BatteryTile(pos: BlockPos, state: BlockState) :
    ApiTile<BatteryTile>(Registry.Tiles.Battery, pos, state, "tile.synth.battery"),
    Nameable, ApiLinkable {
    private val removals = ArrayList<BlockPos>(20)
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(1, ::update)
    override val tank = TrackedTank(0, ::update)
    override val ioPower: Int get() = ((10000) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)
    val links = Links()
    private var tick = 0

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()
        exportPower()
        if (tick >= 20) {
            validateExports()
            update()
            tick = 0
        }
        chargeItem()
        tick++
    }

    /**
     * This will charge the item in the inventory
     */
    private fun chargeItem() {
        val chargeable = items.getStackInSlot(0)
        val cap = chargeable.getCapability(CapabilityEnergy.ENERGY)
        if (cap.isPresent) {
            val other = cap.resolve().get()
            val extracted = energy.extractEnergy(ioPower, true)
            val leftOver = other.receiveEnergy(extracted, false)
            energy.extractEnergy(leftOver, false)
        }
    }

    /**
     * Extracts out power to the links
     */
    private fun exportPower() {
        for (link in links.getLinks()) {
            val other = level?.getBlockEntity(link.key) ?: continue
            for (side in link.value) {
                val cap = other.getCapability(CapabilityEnergy.ENERGY, side)
                if (cap.isPresent) {
                    val extracted = energy.extractEnergy(ioPower, true)
                    val leftOver = cap.resolve().get().receiveEnergy(extracted, false)
                    energy.extractEnergy(leftOver, false)
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
                    val cap = tile.getCapability(CapabilityEnergy.ENERGY, it)
                    if (!cap.isPresent) removals.add(link.key)
                }
            }
        }
        removals.forEach(links::removeLink)
    }

    /**
     * This is used to open up the container menu
     */
    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> BatteryContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

    /**
     * Called when saving nbt data
     */
    override fun onSave(tag: CompoundTag) {
        tag.put("links", links.serializeNBT())
    }

    /**
     * Called when loading the nbt data
     */
    override fun onLoad(tag: CompoundTag) {
        links.deserializeNBT(tag.getCompound("links"))
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
    override fun linkTo(other: BlockPos, face: Direction) {
        links.addLink(other, face)
        update()
    }


    /**
     * Removes our link and syncs with the client
     */
    override fun unlink() {
        links.removeLinks()
        update()
    }

    override fun getRenderBoundingBox(): AABB {
        return INFINITE_EXTENT_AABB
    }

}