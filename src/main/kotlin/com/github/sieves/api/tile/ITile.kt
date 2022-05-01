package com.github.sieves.api.tile

import com.github.sieves.util.*
import net.minecraft.core.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.*
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResult.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.*

/**
 * Pulls out some functionality from the tile entity
 */
interface ITile<T : BlockEntity> {
    @Suppress("UNCHECKED_CAST")
    private val cast: T
        get() = this as T

    /**
     * Extracts the block position from the cast tile entity
     */
    val pos: BlockPos get() = cast.blockPos

    /**
     * Extracts the state from the tile entity
     */
    val state: BlockState get() = cast.blockState

    /**
     * Extract out the world, called world because it then won't class with the getLevel from BlockEntity
     */
    val world: Opt<Level> get() = Opt.ofNullable(cast.level)

    /**
     * Checks to see if we're on the client side or not, will return false by default if the world is not initialized
     */
    val isServerSide: Boolean get() = if (world.isAbsent) true else !world().isClientSide

    /**
     * Used to do additional saving
     */
    fun onSave(tag: CompoundTag) = Unit

    /**
     * Used to do additional loading
     */
    fun onLoad(tag: CompoundTag) = Unit

    /**
     * Invalidation of the tile entity
     */
    fun onInvalidate() = Unit

    /**
     * Called right before the first tick, add initialization logic here if it requires the world to be present
     */
    fun onInit() = Unit

    /**
     * Called upon ticking the server and client tile entities
     */
    fun onTick(level: Level) = Unit

    /**
     * Called upon right-clicking this block
     */
    fun onUse(level: Level, player: Player, itemUsed: ItemStack, direction: Direction): InteractionResult = PASS

    /**
     * Called upon the breaking of the block
     */
    fun onBreak(level: Level, player: Player, willHarvest: Boolean, fluid: FluidState) = Unit

    /**
     * Updates the tile entity
     */
    fun update()

}