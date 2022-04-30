package com.github.sieves.api.tile

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.ApiConfig.Side.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.util.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.InteractionResult.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.*

interface IMultiBlock<T : BlockEntity> {

    @Suppress("UNCHECKED_CAST")
    val self: T
        get() = this as T

    /**
     * The controller
     */
    var ctrlPos: BlockPos

    /**
     * The optional value of the controller
     */
    val ctrl: Opt<ControlTile>


    /**
     * Simple check to see if our controller is valid
     */
    fun controllerValid(): Boolean = ctrl.isPresent


    /**
     * Called upon destroying the block
     */
    fun onDestroy(player: Player, willHarvest: Boolean, fluid: FluidState) {}

    /**
     * Delegate right-clicking on the client to the control tile
     */
    fun onUseClient(player: Player, itemUsed: ItemStack, direction: Direction): InteractionResult {
        return PASS
    }

    /**
     * Delegate right-clicking on the server to the control tile
     */
    fun onUseServer(player: ServerPlayer, itemUsed: ItemStack, direction: Direction): InteractionResult {
        return PASS
    }


    /**
     * Delegate ticking of the server here
     */
    fun onServerTick() {}

    /**
     * Delegate ticking of the client to here
     */
    fun onClientTick() {}

    /**
     * Invalidate capabilities
     */
    fun onInvalidate() {}

    /**
     * Do nbt saving here
     */
    fun onSave(tag: CompoundTag) {}

    /**
     * Load nbt data here
     */
    fun onLoad(tag: CompoundTag) {}

    /**
     * Push update to client
     */
    fun update() {}

    //Front side
    val front: BlockState get() = neighborState(Front)

    //Back side
    val back: BlockState get() = neighborState(Back)

    //right side
    val right: BlockState get() = neighborState(Right)

    //left side
    val left: BlockState get() = neighborState(Front)

    //below side
    val below: BlockState get() = neighborState(Bottom)

    //above side
    val above: BlockState get() = neighborState(Top)

    /**
     * Gets the block state at the given side offset
     */
    fun neighborState(side: Side, horizontal: Boolean = true): BlockState {
        val level = self.level ?: return Blocks.AIR.defaultBlockState()
        val pos = neighborPos(side, horizontal)
        return level.getBlockState(pos)
    }

    /**
     * Gets a block for the given side using the horizontal offset
     */
    fun neighborPos(side: Side, horizontal: Boolean = true): BlockPos {
        val forward = self.blockState.getValue(
            if (horizontal) HorizontalDirectionalBlock.FACING else DirectionalBlock.FACING
        )
        return self.blockPos.offset(
            when (side) {
                Top -> Direction.UP
                Bottom -> Direction.DOWN
                Front -> forward
                Back -> forward.opposite
                Left -> forward.counterClockWise
                Right -> forward.clockWise
            }
        )
    }
}