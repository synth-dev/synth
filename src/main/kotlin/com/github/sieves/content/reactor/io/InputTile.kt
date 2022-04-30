package com.github.sieves.content.reactor.io

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.tile.*
import com.github.sieves.registry.*
import com.github.sieves.util.*
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.items.CapabilityItemHandler

/**
 * Keeps track of the internal input buffer of items being pumped into the controller
 */
class InputTile(blockPos: BlockPos, blockState: BlockState) : ReactorTile<InputTile>(Registry.Tiles.Input, blockPos, blockState) {
    private var tick = 0

    /**
     * Will only tick when the controller is present
     */
    override fun onServerTick() = ctrl.ifPresent {
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
        val front = neighborPos(Side.Front)
        val be = level?.getBlockEntity(front)
        level?.setBlockAndUpdate(blockPos, blockState.setValue(InputBlock.Piped, be?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)?.isPresent == true))
    }

}