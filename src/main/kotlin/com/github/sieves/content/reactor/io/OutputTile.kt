package com.github.sieves.content.reactor.io

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.multiblock.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.registry.*
import com.github.sieves.dsl.*
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.energy.CapabilityEnergy

/**
 * Keeps track of the internal input buffer of items being pumped into the controller
 */
class OutputTile(blockPos: BlockPos, blockState: BlockState) : BaseTile<OutputTile>(Registry.Tiles.Output, blockPos, blockState),
    ISlave<ControlTile, OutputTile> {
    private var tick = 0

    /**Get and set the master instance**/
    override var master: Opt<IMaster<ControlTile>> = Opt.nil()

    /**Used for interactions with the master**/
    override var store: Opt<StructureStore> = Opt.nil()

    /**
     * Will only tick when the controller is present
     */
    override fun onTick(level: Level) {
        if (level.isClientSide || master.isAbsent) return
        if (tick >= 5) {
            tryUpdateState()
            tick = 0
        }
        tick++
    }

    /**
     * Attempts to update the state if possible
     */
    private fun tryUpdateState() {
        val front = getNeighbor(Side.Front)
        val be = level?.getBlockEntity(front)
        level?.setBlockAndUpdate(blockPos, blockState.setValue(InputBlock.Piped, be?.getCapability(CapabilityEnergy.ENERGY)?.isPresent == true))
    }

}