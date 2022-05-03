package com.github.sieves.content.machines.synthesizer

import com.github.sieves.api.ApiBlock
import com.github.sieves.registry.Registry
import com.github.sieves.dsl.join
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class SynthesizerBlock(properties: Properties) :
    ApiBlock<SynthesizerTile>(properties, { Registry.Tiles.Synthesizer }) {

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState()
            .setValue(HorizontalDirectionalBlock.FACING, pContext.horizontalDirection.opposite)
    }

    override fun rotate(pState: BlockState, pRotation: Rotation): BlockState {
        return pState.setValue(
            HorizontalDirectionalBlock.FACING,
            pRotation.rotate(pState.getValue(HorizontalDirectionalBlock.FACING))
        )
    }

    override fun mirror(pState: BlockState, pMirror: Mirror): BlockState {
        return pState.rotate(pMirror.getRotation(pState.getValue(HorizontalDirectionalBlock.FACING)))
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(HorizontalDirectionalBlock.FACING)
    }

    override fun getLightEmission(state: BlockState?, level: BlockGetter?, pos: BlockPos?): Int {
        val entity = pos?.let { level?.getBlockEntity(it) } ?: return 0
        if (entity !is SynthesizerTile) return 0
        return ((entity.progress / entity.targetProgress.toFloat()) * 15).toInt()
    }



    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return when (pState.getValue(HorizontalDirectionalBlock.FACING)) {
            Direction.NORTH -> north
            Direction.EAST -> east
            Direction.WEST -> west
            Direction.SOUTH -> south
            else -> north
        }
    }

    override fun getShadeBrightness(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos): Float {
        return 0.8f
    }

    private val south: VoxelShape = Shapes.empty()
        .join(Shapes.box(0.0625, 0.1875, 0.0625, 0.9375, 0.25, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.9375, 0.0625, 0.9375, 1.0, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.0625, 0.9375, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.0625, 0.125, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.25, 0.0625, 0.875, 0.9375, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.25, 0.875, 0.625, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.25, 0.875, 0.375, 0.9375, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.25, 0.875, 0.875, 0.9375, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.0625, 0.9375, 0.1875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.8125, 0.9375, 0.1875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.0625, 0.1875, 0.1875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.8125, 0.1875, 0.1875, 0.9375), BooleanOp.OR)

    private val west: VoxelShape = Shapes.empty()
        .join(Shapes.box(0.0625, 0.1875, 0.0625, 0.9375, 0.25, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.9375, 0.0625, 0.9375, 1.0, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.875, 0.9375, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.0625, 0.9375, 0.9375, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.125, 0.9375, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.375, 0.125, 0.9375, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.25, 0.125, 0.125, 0.9375, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.25, 0.625, 0.125, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.8125, 0.9375, 0.1875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.8125, 0.1875, 0.1875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.0625, 0.9375, 0.1875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.0625, 0.1875, 0.1875, 0.1875), BooleanOp.OR)


    private val east: VoxelShape = Shapes.empty()
        .join(Shapes.box(0.0625, 0.1875, 0.0625, 0.9375, 0.25, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.9375, 0.0625, 0.9375, 1.0, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.0625, 0.9375, 0.9375, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.875, 0.9375, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.125, 0.125, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.375, 0.9375, 0.9375, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.625, 1.0, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.125, 1.0, 0.9375, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.0625, 0.1875, 0.1875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.0625, 0.9375, 0.1875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.8125, 0.1875, 0.1875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.8125, 0.9375, 0.1875, 0.9375), BooleanOp.OR)

    private val north: VoxelShape = Shapes.empty()
        .join(Shapes.box(0.375, 0.25, 0.0625, 0.625, 0.9375, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.0625, 0.1875, 0.1875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.8125, 0.9375, 0.1875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.8125, 0.1875, 0.1875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.0625, 0.9375, 0.1875, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.1875, 0.0625, 0.9375, 0.25, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.0625, 0.125, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.0625, 0.9375, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.25, 0.875, 0.875, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.25, 0.0, 0.875, 0.9375, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.25, 0.0, 0.375, 0.9375, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.9375, 0.0625, 0.9375, 1.0, 0.9375), BooleanOp.OR)

    companion object {
    }

}