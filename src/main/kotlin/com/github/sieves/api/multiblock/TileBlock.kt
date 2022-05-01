package com.github.sieves.api.multiblock

import com.github.sieves.api.tile.*
import net.minecraft.core.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.material.*
import net.minecraft.world.phys.*

abstract class TileBlock<T : BlockEntity>(
    /**Needed to provide our block entity creation**/
    private val tileType: () -> BlockEntityType<T>,
    /**The speed at which this tile entity will tick, if it's -1 it won't tick**/
    tickInterval: Int = -1,
    properties: Properties = Properties.of(Material.HEAVY_METAL).strength(3f)
) : Block(properties),
    EntityBlock {
    private val ticker = Ticker<T>(tickInterval)

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity?> getTicker(pLevel: Level, pState: BlockState, pBlockEntityType: BlockEntityType<T>): BlockEntityTicker<T>? {
        if (ticker.tickInterval >= 0) return ticker as BlockEntityTicker<T>
        return null
    }

    /**
     * Called when a player removes a block.  This is responsible for
     * actually destroying the block, and the block is intact at time of call.
     * This is called regardless of whether the player can harvest the block or
     * not.
     *
     * Return true if the block is actually destroyed.
     *
     * Note: When used in multiplayer, this is called on both client and
     * server sides!
     *
     * @param state The current state.
     * @param level The current level
     * @param player The player damaging the block, may be null
     * @param pos Block positions in level
     * @param willHarvest True if Block.harvestBlock will be called after this, if the return in true.
     * Can be useful to delay the destruction of tile entities till after harvestBlock
     * @param fluid The current fluid state at current position
     * @return True if the block is actually destroyed.
     */
    override fun onDestroyedByPlayer(state: BlockState?, level: Level?, pos: BlockPos?, player: Player?, willHarvest: Boolean, fluid: FluidState?): Boolean {
        val tile = pos?.let { level?.getBlockEntity(it) } ?: return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
        if (tile !is ITile<*>) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
        level?.let { lvl -> player?.let { fluid?.let { it1 -> tile.onBreak(lvl, it, willHarvest, it1) } } }
        return super.onDestroyedByPlayer(
            state, level, pos, player, willHarvest, fluid
        )
    }

    /**
     * Here we pass along our use state to our tile entity
     */
    override fun use(pState: BlockState, pLevel: Level, pPos: BlockPos, pPlayer: Player, pHand: InteractionHand, pHit: BlockHitResult): InteractionResult {
        val tile = pLevel.getBlockEntity(pPos)
        if (tile !is ITile<*>) return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit)
        return tile.onUse(pLevel, pPlayer, pPlayer.getItemInHand(pHand), pHit.direction)
    }

    private class Ticker<T : BlockEntity>(val tickInterval: Int) : BlockEntityTicker<T> {
        /**Keep track of the current tick for this instance**/
        private var tick = 0

        /**
         * This will tick based upon our interval
         */
        override fun tick(pLevel: Level, pPos: BlockPos, pState: BlockState, pBlockEntity: T) {
            if (tickInterval < 0) return
            if (pBlockEntity is ITile<*>) {
                if (tickInterval == 0 || tick >= tickInterval) {
                    pBlockEntity.onTick(pLevel)
                    tick = 0
                }
                tick++
            }
        }

    }

    /**
     * Create our tile entity
     */
    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity? = tileType().create(pPos, pState)
}