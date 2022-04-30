package com.github.sieves.content.reactor.control

import com.github.sieves.api.tile.*
import com.github.sieves.registry.Registry.Tiles
import com.github.sieves.util.*
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*

class ControlBlock : Block(Properties.of(Material.HEAVY_METAL)), EntityBlock {
    init {
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, NORTH).setValue(Formed, false))
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, pContext.horizontalDirection).setValue(Formed, false)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(HorizontalDirectionalBlock.FACING).add(Formed)
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity? = Tiles.Control.create(pPos, pState)

    override fun onDestroyedByPlayer(state: BlockState?, level: Level?, pos: BlockPos?, player: Player?, willHarvest: Boolean, fluid: FluidState?): Boolean {
        val tile = pos?.let { level?.getBlockEntity(it) } ?: return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
        if (tile !is IMultiBlock<*>) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
        player?.let { fluid?.let { it1 -> tile.onDestroy(it, willHarvest, it1) } }
        return super.onDestroyedByPlayer(
            state, level, pos, player, willHarvest, fluid
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity?> getTicker(pLevel: Level, pState: BlockState, pBlockEntityType: BlockEntityType<T>): BlockEntityTicker<T> =
        ReactorTile.Ticker as BlockEntityTicker<T>

    override fun use(pState: BlockState, pLevel: Level, pPos: BlockPos, pPlayer: Player, pHand: InteractionHand, pHit: BlockHitResult): InteractionResult {
        val tile = pLevel.getBlockEntity(pPos)
        if (tile !is IMultiBlock<*>) return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit)
        return if (pLevel.isClientSide) tile.onUseClient(pPlayer, pPlayer.getItemInHand(pHand), pHit.direction)
        else tile.onUseServer(pPlayer as ServerPlayer, pPlayer.getItemInHand(pHand), pHit.direction)
    }

    private val east =
        Shapes.empty().join(Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0), BooleanOp.OR).join(Shapes.box(0.5, 0.3125, 0.3125, 0.5625, 0.8125, 0.6875), BooleanOp.OR)
    private val west =
        Shapes.empty().join(Shapes.box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0), BooleanOp.OR).join(Shapes.box(0.4375, 0.3125, 0.3125, 0.5, 0.8125, 0.6875), BooleanOp.OR)

    private val north =
        Shapes.empty().join(Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 1.0), BooleanOp.OR).join(Shapes.box(0.3125, 0.3125, 0.4375, 0.6875, 0.8125, 0.5), BooleanOp.OR)
    private val south =
        Shapes.empty().join(Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.5), BooleanOp.OR).join(Shapes.box(0.3125, 0.3125, 0.5, 0.6875, 0.8125, 0.5625), BooleanOp.OR)

    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    override fun getShape(
        pState: BlockState, pLevel: BlockGetter, pPos: BlockPos, pContext: CollisionContext
    ): VoxelShape {
        return if (pState.getValue(Formed)) {
            when (pState.getValue(HorizontalDirectionalBlock.FACING)) {
                NORTH -> north
                SOUTH -> south
                WEST -> west
                EAST -> east
                else -> north
            }
        } else Shapes.block()
    }


    companion object {
        val Formed: BooleanProperty = BooleanProperty.create("formed")
    }

}