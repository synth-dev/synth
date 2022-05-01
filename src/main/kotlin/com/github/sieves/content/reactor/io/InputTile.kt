package com.github.sieves.content.reactor.io

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.multiblock.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Items
import com.github.sieves.util.*
import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.*
import net.minecraftforge.items.CapabilityItemHandler

/**
 * Keeps track of the internal input buffer of items being pumped into the controller
 */
class InputTile(blockPos: BlockPos, blockState: BlockState) : BaseTile<InputTile>(Registry.Tiles.Input, blockPos, blockState),
    ISlave<ControlTile, InputTile> {
    /**Get and set the master instance**/
    override var master: Opt<IMaster<ControlTile>> = Opt.nil()

    /**Used for interactions with the master**/
    override var store: Opt<StructureStore> = Opt.nil()

    /**Our internal item buffer, we keep create a delegated item handler that has 10 slots**/
    private val items by handlerOf<Delegates.Items>("items", 10)

    /**Store our fluids buffer with a 100 bucket buffer**/
    private val fluids by handlerOf<Delegates.Fluids>("fluids", 100_000)

    /**Store an internal buffer of 100k fe**/
    private val energy by handlerOf<Delegates.Energy>("energy", 100_000)

    /**Keep track of tick to do logic at fixed rate**/
    private var tick = 0

    /**
     * Will only tick when the controller is present
     */
    override fun onTick(level: Level) {
        if (level.isClientSide || master.isAbsent) return
        tryUpdateState()
        extract()
    }

    /**
     * Extracts the items from the touching inventories to our local inventory
     */
    private fun extract() {
        val forward = blockState.getValue(HorizontalDirectionalBlock.FACING)
        val tile = level?.getBlockEntity(blockPos.offset(forward)) ?: return
        val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, forward.opposite)
        if (cap.isPresent) {
            val inv = cap.resolve().get()
            for (slot in 0 until inv.slots) {
                val stack = inv.getStackInSlot(slot)
                if (stack.`is`(Items.FluxDust)) {
                    val items = inv.extractItem(slot, 1, true)
                    if (!items.isEmpty) {
                        val inserted = this.items().insertItem(items, true)
                        if (inserted.isEmpty) {
                            this.items().insertItem(items, false)
                            inv.extractItem(slot, 1, false)
                        }
                    }
                }
            }
        }
        val fluids = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
        if (fluids.isPresent) {
            val tank = fluids.resolve().get()
            val result = tank.drain(1000, SIMULATE)
            tank.drain(this.fluids().fill(result, EXECUTE), EXECUTE)
        }
        val energy = tile.getCapability(CapabilityEnergy.ENERGY)
        if (energy.isPresent) {
            val cell = energy.resolve().get()
            if (cell.canExtract()) {
                val result = cell.extractEnergy(1000, true)
                cell.extractEnergy(this.energy().receiveEnergy(result, false), false)
            }
        }

    }

    /**
     * Attempts to update the state if possible
     */
    private fun tryUpdateState() {
        val front = getNeighbor(Side.Front)
        val be = level?.getBlockEntity(front)
        val valid = be?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)?.isPresent == true
                || be?.getCapability(CapabilityEnergy.ENERGY)?.isPresent == true
                || be?.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)?.isPresent == true
        level?.setBlockAndUpdate(
            blockPos, blockState.setValue(InputBlock.Piped, valid)
        )
    }

    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return items.cast()
        }
        return super.getCapability(cap, side)
    }


}