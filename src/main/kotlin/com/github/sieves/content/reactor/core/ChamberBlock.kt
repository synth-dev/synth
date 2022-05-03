package com.github.sieves.content.reactor.core

import com.github.sieves.api.multiblock.*
import com.github.sieves.dsl.*
import com.github.sieves.registry.Registry.Tiles
import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.RenderShape.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.shapes.*

class ChamberBlock : TileBlock<ChamberTile>({ Tiles.Chamber }, 0) {

    override fun getShape(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos, pContext: CollisionContext): VoxelShape = shape
    override fun getRenderShape(pState: BlockState): RenderShape = ENTITYBLOCK_ANIMATED
    private val shape = Shapes.empty()
        .join(Shapes.box(0.125, 0.0, 0.375, 0.875, 0.0625, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0, 0.3125, 0.8125, 0.0625, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.0, 0.25, 0.75, 0.0625, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.0, 0.1875, 0.6875, 0.0625, 0.25), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.0, 0.125, 0.625, 0.0625, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0, 0.625, 0.8125, 0.0625, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.0, 0.6875, 0.75, 0.0625, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.0, 0.75, 0.6875, 0.0625, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.0, 0.8125, 0.625, 0.0625, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.9375, 0.375, 0.875, 1.0, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.9375, 0.3125, 0.8125, 1.0, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.9375, 0.25, 0.75, 1.0, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.9375, 0.1875, 0.6875, 1.0, 0.25), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.9375, 0.125, 0.625, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.9375, 0.625, 0.8125, 1.0, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.9375, 0.6875, 0.75, 1.0, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.9375, 0.75, 0.6875, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.9375, 0.8125, 0.625, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0, 0.3125, 0.1875, 1.0, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.0, 0.125, 0.375, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.375, 0.125, 0.25, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.75, 0.375, 0.125, 1.0, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.109375, 0.25, 0.40625, 0.125, 0.75, 0.59375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.59375, 0.125, 0.75, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.25, 0.375, 0.125, 0.75, 0.40625), BooleanOp.OR)
        .join(Shapes.box(0.40625, 0.25, 0.875, 0.59375, 0.75, 0.890625), BooleanOp.OR)
        .join(Shapes.box(0.59375, 0.25, 0.875, 0.625, 0.75, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.25, 0.875, 0.40625, 0.75, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.0, 0.875, 0.625, 0.25, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.75, 0.875, 0.625, 1.0, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.0, 0.8125, 0.375, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0, 0.625, 0.1875, 1.0, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.0, 0.125, 0.6875, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.3125, 0.875, 1.0, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.0, 0.0625, 0.625, 0.25, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.75, 0.0625, 0.625, 1.0, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.40625, 0.25, 0.109375, 0.59375, 0.75, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.25, 0.0625, 0.40625, 0.75, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.59375, 0.25, 0.0625, 0.625, 0.75, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.390625, 0.890625, 0.75, 0.59375), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.375, 0.9375, 0.75, 0.390625), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.25, 0.59375, 0.9375, 0.75, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.625, 0.875, 1.0, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.125, 0.1875, 0.75, 0.875, 0.25), BooleanOp.OR)
        .join(Shapes.box(0.75, 0.125, 0.25, 0.8125, 0.875, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.0, 0.8125, 0.6875, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.0, 0.375, 0.9375, 0.25, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.75, 0.375, 0.9375, 1.0, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.75, 0.875, 0.25, 0.8125, 1.0, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.875, 0.1875, 0.75, 1.0, 0.25), BooleanOp.OR)
        .join(Shapes.box(0.75, 0.0, 0.25, 0.8125, 0.125, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.0, 0.1875, 0.75, 0.125, 0.25), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.125, 0.75, 0.75, 0.875, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.75, 0.125, 0.6875, 0.8125, 0.875, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.875, 0.75, 0.75, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.75, 0.875, 0.6875, 0.8125, 1.0, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.6875, 0.0, 0.75, 0.75, 0.125, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.75, 0.0, 0.6875, 0.8125, 0.125, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.125, 0.1875, 0.3125, 0.875, 0.25), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.125, 0.25, 0.25, 0.875, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.875, 0.1875, 0.3125, 1.0, 0.25), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.875, 0.25, 0.25, 1.0, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.0, 0.1875, 0.3125, 0.125, 0.25), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0, 0.25, 0.25, 0.125, 0.3125), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.125, 0.6875, 0.25, 0.875, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.125, 0.75, 0.3125, 0.875, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.875, 0.6875, 0.25, 1.0, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.875, 0.75, 0.3125, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0, 0.6875, 0.25, 0.125, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.0, 0.75, 0.3125, 0.125, 0.8125), BooleanOp.OR)

}