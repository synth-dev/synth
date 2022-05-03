package com.github.sieves.content.reactor.control

import com.github.sieves.api.multiblock.*
import com.github.sieves.registry.Registry.Tiles
import com.github.sieves.dsl.*
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.shapes.*

class ControlBlock : TileBlock<ControlTile>({ Tiles.Control }, 0) {
    init {
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, NORTH).setValue(Formed, false))
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, pContext.horizontalDirection).setValue(Formed, false)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(HorizontalDirectionalBlock.FACING).add(Formed)
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