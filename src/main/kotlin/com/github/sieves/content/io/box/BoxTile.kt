package com.github.sieves.content.io.box

import com.github.sieves.*
import com.github.sieves.api.*
import com.github.sieves.api.caps.*
import com.github.sieves.content.io.link.*
import com.github.sieves.content.machines.materializer.*
import com.github.sieves.registry.Registry
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*
import net.minecraftforge.energy.*
import net.minecraftforge.fluids.capability.*
import net.minecraftforge.fluids.capability.templates.*
import net.minecraftforge.items.*
import net.minecraftforge.network.*
import kotlin.math.*

class BoxTile(pos: BlockPos, state: BlockState) :
    ApiTile<BoxTile>(Registry.Tiles.Box, pos, state, "tile.synth.box"),
    Nameable, ApiLinkable {
    private val removals = ArrayList<BlockPos>(20)
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(24, ::update)
    override val tank = TrackedTank(0, ::update)//250 buckets base
    private var tick = 0
    val links = Links()
    val powerCost: Int get() = ((links.getLinks().size * 600) / configuration.efficiencyModifier).roundToInt()
    val sleepTime: Int get() = ((20 * 60) / configuration.speedModifier).roundToInt()
    override val ioPower: Int get() = ((links.getLinks().size * 600) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.speedModifier.roundToInt()) * 3) + 1)


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
                            if (other is MaterializerTile) {
                                val result = cap.resolve().get().insertItem(0, extracted, true)
                                if (result.isEmpty) {
                                    items.extractItem(slot, extracted.count, false)
                                    cap.resolve().get().insertItem(0, extracted, false)
                                    break
                                }
                            } else {
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
     * This is used to open up the container menu
     */
    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> BoxContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
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