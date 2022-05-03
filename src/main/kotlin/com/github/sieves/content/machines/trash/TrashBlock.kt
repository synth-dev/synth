package com.github.sieves.content.machines.trash

import com.github.sieves.registry.Registry
import com.github.sieves.dsl.join
import net.minecraft.core.BlockPos
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class TrashBlock(properties: Properties) :
    com.github.sieves.api.ApiBlock<TrashTile>(properties, { Registry.Tiles.Trash }) {

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, pContext.horizontalDirection.opposite)
    }

    override fun rotate(pState: BlockState, pRotation: Rotation): BlockState {
        return pState.setValue(
            HorizontalDirectionalBlock.FACING, pRotation.rotate(pState.getValue(
                HorizontalDirectionalBlock.FACING)))
    }

    override fun mirror(pState: BlockState, pMirror: Mirror): BlockState {
        return pState.rotate(pMirror.getRotation(pState.getValue(HorizontalDirectionalBlock.FACING)))
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(HorizontalDirectionalBlock.FACING)
    }

    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape = shape

    private val shape = Shapes.empty().join(Shapes.box(0.078125, 0.0, 0.078125, 0.921875, 0.0625, 0.921875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0625, 0.1875, 0.8125, 0.125, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.125, 0.1875, 0.21875, 1.0, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.125, 0.6875, 0.21875, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.125, 0.3125, 0.1875, 1.0, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.125, 0.3125, 0.84375, 1.0, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.78125, 0.125, 0.1875, 0.8125, 1.0, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.78125, 0.125, 0.6875, 0.8125, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.125, 0.15625, 0.6875, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.21875, 0.125, 0.1875, 0.3125, 1.0, 0.21875), BooleanOp.OR)
        .join(Shapes.box(0.65625, 0.125, 0.1875, 0.78125, 1.0, 0.21875), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.125, 0.8125, 0.6875, 1.0, 0.84375), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.125, 0.78125, 0.78125, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.21875, 0.125, 0.78125, 0.34375, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.875, 0.1875, 0.1875, 0.96875, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.875, 0.3125, 0.15625, 0.96875, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.875, 0.6875, 0.1875, 0.96875, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.875, 0.09375, 0.8125, 0.96875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.875, 0.09375, 0.3125, 0.96875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.875, 0.09375, 0.6875, 0.96875, 0.15625), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.875, 0.6875, 0.90625, 0.96875, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.875, 0.1875, 0.90625, 0.96875, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.84375, 0.875, 0.3125, 0.90625, 0.96875, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.875, 0.8125, 0.3125, 0.96875, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.875, 0.8125, 0.8125, 0.96875, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.875, 0.84375, 0.6875, 0.96875, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.875, 0.8125, 0.9375, 0.96875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.875, 0.0625, 0.9375, 0.96875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.875, 0.0625, 0.1875, 0.96875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.875, 0.8125, 0.1875, 0.96875, 0.9375), BooleanOp.OR)

    override fun getShadeBrightness(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos): Float {
        return 0.6f
    }

}