package com.github.sieves.api.tile

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.ApiConfig.Side.*
import com.github.sieves.dsl.*
import mcjty.theoneprobe.api.*
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.nbt.*
import net.minecraft.world.*
import net.minecraft.world.InteractionResult.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.material.*
import net.minecraft.world.phys.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*

/**
 * Pulls out some functionality from the tile entity
 */
interface ITile<T : BlockEntity> {
    @Suppress("UNCHECKED_CAST")
    val cast: T
        get() = this as T

    /**
     * To invoke operator will get the casted instance
     */
    operator fun invoke(): T = cast

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
    val world: Opt<Level> get() = Opt.ofNilable(cast.level)

    /**
     * creates a bounding box around the block withing a 32x32x32 radius
     */
    val aabb: AABB get() = AABB(pos.offset(-32, -32, -32), pos.offset(31, 31, 31))


    /**
     * Checks to see if we're on the client side or not, will return false by default if the world is not initialized
     */
    val isServerSide: Boolean get() = if (world.isAbsent) true else !world().isClientSide

    /**
     * Compute the forward direction for the tile based upon the block state
     */
    fun forwardDir(): Direction =
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            state.getValue(HorizontalDirectionalBlock.FACING)
        else if (state.hasProperty(DirectionalBlock.FACING))
            state.getValue(DirectionalBlock.FACING)
        else NORTH

    /**
     * Gets a direction relative to the block states facing direction
     */
    fun relativeDir(side: Side): Direction {
        val forward = forwardDir()
        return when (side) {
            Top -> if (forward == UP || forward == DOWN) NORTH else UP
            Bottom -> if (forward == UP || forward == DOWN) NORTH.opposite else DOWN
            Front -> if (forward == UP || forward == DOWN) NORTH else forward
            Back -> if (forward == UP || forward == DOWN) SOUTH else forward.opposite
            Left -> if (forward == UP || forward == DOWN) NORTH.counterClockWise else forward.counterClockWise
            Right -> if (forward == UP || forward == DOWN) NORTH.clockWise else forward.clockWise
        }
    }

    /**
     * Get the capability for the given direction
     */
    fun <T> neighborCap(side: Side, capability: Capability<T>): LazyOptional<T> {
        val dir = relativeDir(side)
        if (!world) return LazyOptional.empty()
        val neighborPos = pos.offset(dir)
        return world().getCapability(capability, dir.opposite)
    }

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

    /**
     * render the top plugin information for this tile, only on client side
     */
    fun renderTop(info: IProbeInfo, mode: ProbeMode, data: IProbeHitData, player: Player)


    /**
     * Gets all of the players contained withing the bounding box
     */
    @Suppress("UNCHECKED_CAST")
    val containedPlayers: List<Player>
        get() = if (!world) emptyList<Player>() else world().getEntitiesOfClass(
            LivingEntity::class.java, aabb
        ) { it is Player } as List<Player>

}