package com.github.sieves.content.io.fluids

import com.github.sieves.api.*
import com.github.sieves.api.caps.*
import com.github.sieves.content.io.link.Links
import com.github.sieves.registry.Registry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.fluids.*
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.*
import net.minecraftforge.fluids.capability.templates.*
import net.minecraftforge.network.NetworkHooks
import kotlin.math.roundToInt

class FluidsTile(pos: BlockPos, state: BlockState) :
    ApiTile<FluidsTile>(Registry.Tiles.Fluids, pos, state, "container.synth.tank"), ApiLinkable {
    private val removals = ArrayList<BlockPos>(20)
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(1, ::update)
    override val tank: FluidTank = TrackedTank(250_000, ::update) //TODO: add capacity upgrade
    val powerCost: Int get() = (600 + (links.getLinks().size * 600) / configuration.efficiencyModifier).roundToInt()
    override val ioPower: Int get() = ((links.getLinks().size * 600) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = 1000 + (configuration.speedModifier * 1000).roundToInt() // base level of
    private var tick = 0
    val links = Links()

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()
        if (extractPower()) {
            if (tick % 5 == 0) {
                exportFluids() // 4 times per second
                pumpFluids()
            }
            if (tick >= 20) { //once per second
                validateExports()
                tick = 0
            }
            tick++
            update()
        }
    }


    /**
     * Pump fluids when auto importing
     */
    private fun pumpFluids() {
        if (!configuration.autoImport) return
        for (key in Direction.values()) {
            val value = getConfig()[key]
            if (value.canImportFluid) {
                val pos = blockPos.offset(key.normal)
                val state = level?.getBlockState(pos) ?: continue
                val fluids = state.fluidState
                if (!fluids.isEmpty) {
                    val type = fluids.type
                    val result = this.tank.fill(FluidStack(type, 1000), SIMULATE)
                    if (result == 1000) {
                        this.tank.fill(FluidStack(type, 1000), EXECUTE)
                        level?.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
                    }
                }
            }
        }
    }

    /**
     * Extracts out fluids to the links
     */
    private fun exportFluids() {
        for (link in links.getLinks()) {
            val otherTile = level?.getBlockEntity(link.key) ?: continue
            for (side in link.value) {
                val cap = otherTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    val extracted = tank.drain(this.ioRate, SIMULATE)
                    val filled = other.fill(extracted, SIMULATE)
                    if (filled != 0) {
                        other.fill(tank.drain(filled, EXECUTE), EXECUTE)
                    }
                }
            }
        }
    }

    private fun extractPower(): Boolean {
        val extracted = energy.extractEnergy(powerCost, true)
        if (extracted != powerCost) return false
        energy.extractEnergy(powerCost, false)
        return true
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
                    val cap = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, it)
                    if (!cap.isPresent) removals.add(link.key)
                }
            }
        }
        removals.forEach(links::remove)
    }

    /**
     * This is used to open up the container menu
     */
    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> FluidsContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

    /**
     * Adds our link and syncs with the client
     */
    override fun linkTo(other: BlockPos, face: Direction) {
        links.add(other, face)
        update()
    }

    /**
     * Removes our link and syncs with the client
     */
    override fun unlink() {
        links.removeLinks()
        update()
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

}