package com.github.sieves.content.machines.farmer

import com.github.sieves.api.ApiBlock
import com.github.sieves.content.machines.forester.ForesterTile
import com.github.sieves.registry.Registry
import com.github.sieves.util.join
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class FarmerBlock(properties: Properties) :
    com.github.sieves.api.ApiBlock<FarmerTile>(properties, { Registry.Tiles.Farmer }) {

    private val shape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.125, 0.0625, 0.9375, 0.4375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.390625, 0.875, 0.390625, 0.609375, 1.0, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.78125, 0.359375, 0.640625, 0.875, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.703125, 0.3125, 0.6875, 0.78125, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.4375, 0.15625, 0.84375, 0.71875, 0.84375), BooleanOp.OR)


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