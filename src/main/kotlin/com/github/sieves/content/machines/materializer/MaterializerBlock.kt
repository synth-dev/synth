package com.github.sieves.content.machines.materializer

import com.github.sieves.api.*
import com.github.sieves.registry.Registry.Tiles
import com.github.sieves.util.*
import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.shapes.*

class MaterializerBlock(properties: Properties) : ApiBlock<MaterializerTile>(properties, { Tiles.Materializer }) {

    private val shape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.125, 0.0625, 0.9375, 0.4375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.390625, 0.875, 0.390625, 0.609375, 1.0, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.78125, 0.359375, 0.640625, 0.875, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.703125, 0.3125, 0.6875, 0.78125, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.4375, 0.15625, 0.84375, 0.71875, 0.84375), BooleanOp.OR)


    override fun getStateDefinition(): StateDefinition<Block, BlockState> {
        return super.getStateDefinition()
    }


    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape = shape

    override fun getShadeBrightness(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos): Float {
        return 0.6f
    }
}