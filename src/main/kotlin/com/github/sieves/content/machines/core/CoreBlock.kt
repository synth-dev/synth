package com.github.sieves.content.machines.core

import com.github.sieves.registry.Registry
import com.github.sieves.util.join
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class CoreBlock(properties: Properties) :
    com.github.sieves.api.ApiBlock<CoreTile>(properties, { Registry.Tiles.Core }) {
    val north = Shapes.empty()
        .join(Shapes.box(0.03125, 0.03125, 0.1250000000000001, 0.125, 0.125, 0.9375000000000001), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.03125, 0.1250000000000001, 0.96875, 0.125, 0.9375000000000001), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.125, 0.06250000000000011, 0.96875, 0.875, 0.1250000000000001), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.875, 0.1250000000000001, 0.96875, 0.96875, 0.9375000000000001), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.125, 0.9375, 0.984375, 0.875, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.125, 0.06250000000000011, 0.125, 0.875, 0.1250000000000001), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.125, 0.9375, 0.125, 0.875, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.875, 0.1250000000000001, 0.125, 0.96875, 0.9375000000000001), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.03125, 0.06250000000000011, 0.875, 0.125, 0.1250000000000001), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.875, 0.06250000000000011, 0.875, 0.96875, 0.1250000000000001), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.875, 0.9375, 0.984375, 0.984375, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.015625, 0.9375, 0.984375, 0.125, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.125, 0.125, 0.09375, 0.875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.90625, 0.125, 0.125, 0.9375, 0.875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.90625, 0.125, 0.875, 0.9375, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.125, 0.09375, 0.359375, 0.359375, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.640625, 0.09375, 0.359375, 0.875, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.640625, 0.09375, 0.875, 0.875, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.125, 0.09375, 0.875, 0.359375, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.125, 0.96875, 0.875, 0.359375, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.125, 0.96875, 0.359375, 0.359375, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.640625, 0.96875, 0.359375, 0.875, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.640625, 0.96875, 0.875, 0.875, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.0, 0.0, 0.125, 0.125, 0.1250000000000001), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.0, 0.0, 1.0, 0.125, 0.1250000000000001), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.875, 0.0, 1.0, 1.0, 0.1250000000000001), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.875, 0.0, 0.125, 1.0, 0.1250000000000001), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0625, 0.125, 0.875, 0.09375, 0.9375), BooleanOp.OR)


    val south = Shapes.empty()
        .join(Shapes.box(0.875, 0.03125, 0.06249999999999989, 0.96875, 0.125, 0.8749999999999999), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.03125, 0.06249999999999989, 0.125, 0.125, 0.8749999999999999), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.125, 0.8749999999999999, 0.125, 0.875, 0.9374999999999999), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.875, 0.06249999999999989, 0.125, 0.96875, 0.8749999999999999), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.125, 0.0, 0.125, 0.875, 0.0625), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.125, 0.8749999999999999, 0.96875, 0.875, 0.9374999999999999), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.125, 0.0, 0.984375, 0.875, 0.0625), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.875, 0.06249999999999989, 0.96875, 0.96875, 0.8749999999999999), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.03125, 0.8749999999999999, 0.875, 0.125, 0.9374999999999999), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.875, 0.8749999999999999, 0.875, 0.96875, 0.9374999999999999), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.875, 0.0, 0.984375, 0.984375, 0.0625), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.015625, 0.0, 0.984375, 0.125, 0.0625), BooleanOp.OR)
        .join(Shapes.box(0.90625, 0.125, 0.0625, 0.9375, 0.875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.125, 0.0625, 0.09375, 0.875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.90625, 0.0625, 0.875, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.125, 0.875, 0.875, 0.359375, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.640625, 0.875, 0.875, 0.875, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.640625, 0.875, 0.375, 0.875, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.125, 0.875, 0.359375, 0.359375, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.125, 0.0, 0.359375, 0.359375, 0.03125), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.125, 0.0, 0.875, 0.359375, 0.03125), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.640625, 0.0, 0.875, 0.875, 0.03125), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.640625, 0.0, 0.375, 0.875, 0.03125), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.0, 0.8749999999999999, 1.0, 0.125, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.0, 0.8749999999999999, 0.125, 0.125, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.875, 0.8749999999999999, 0.125, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.875, 0.8749999999999999, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0625, 0.0625, 0.875, 0.09375, 0.875), BooleanOp.OR)


    val east = Shapes.empty()
        .join(Shapes.box(0.06249999999999989, 0.03125, 0.03125, 0.8749999999999999, 0.125, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.06249999999999989, 0.03125, 0.875, 0.8749999999999999, 0.125, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.8749999999999999, 0.125, 0.875, 0.9374999999999999, 0.875, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.06249999999999989, 0.875, 0.875, 0.8749999999999999, 0.96875, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.125, 0.875, 0.0625, 0.875, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.8749999999999999, 0.125, 0.03125, 0.9374999999999999, 0.875, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.125, 0.015625, 0.0625, 0.875, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.06249999999999989, 0.875, 0.03125, 0.8749999999999999, 0.96875, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.8749999999999999, 0.03125, 0.125, 0.9374999999999999, 0.125, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8749999999999999, 0.875, 0.125, 0.9374999999999999, 0.96875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.875, 0.015625, 0.0625, 0.984375, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.015625, 0.015625, 0.0625, 0.125, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.125, 0.0625, 0.875, 0.875, 0.09375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.125, 0.90625, 0.875, 0.875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.90625, 0.125, 0.875, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.125, 0.125, 0.90625, 0.359375, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.640625, 0.125, 0.90625, 0.875, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.640625, 0.625, 0.90625, 0.875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.125, 0.640625, 0.90625, 0.359375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.125, 0.640625, 0.03125, 0.359375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.125, 0.125, 0.03125, 0.359375, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.640625, 0.125, 0.03125, 0.875, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.640625, 0.625, 0.03125, 0.875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8749999999999999, 0.0, 0.0, 1.0, 0.125, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.8749999999999999, 0.0, 0.875, 1.0, 0.125, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.8749999999999999, 0.875, 0.875, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.8749999999999999, 0.875, 0.0, 1.0, 1.0, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0625, 0.125, 0.875, 0.09375, 0.875), BooleanOp.OR)


    val west = Shapes.empty()
        .join(Shapes.box(0.1250000000000001, 0.03125, 0.875, 0.9375000000000001, 0.125, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.1250000000000001, 0.03125, 0.03125, 0.9375000000000001, 0.125, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.06250000000000011, 0.125, 0.03125, 0.1250000000000001, 0.875, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.1250000000000001, 0.875, 0.03125, 0.9375000000000001, 0.96875, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.9375, 0.125, 0.015625, 1.0, 0.875, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.06250000000000011, 0.125, 0.875, 0.1250000000000001, 0.875, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.9375, 0.125, 0.875, 1.0, 0.875, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.1250000000000001, 0.875, 0.875, 0.9375000000000001, 0.96875, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.06250000000000011, 0.03125, 0.125, 0.1250000000000001, 0.125, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.06250000000000011, 0.875, 0.125, 0.1250000000000001, 0.96875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.9375, 0.875, 0.015625, 1.0, 0.984375, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.9375, 0.015625, 0.015625, 1.0, 0.125, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.125, 0.90625, 0.9375, 0.875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.125, 0.0625, 0.9375, 0.875, 0.09375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.90625, 0.125, 0.9375, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.125, 0.640625, 0.125, 0.359375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.640625, 0.640625, 0.125, 0.875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.640625, 0.125, 0.125, 0.875, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.125, 0.125, 0.125, 0.359375, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.96875, 0.125, 0.125, 1.0, 0.359375, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.96875, 0.125, 0.640625, 1.0, 0.359375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.96875, 0.640625, 0.640625, 1.0, 0.875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.96875, 0.640625, 0.125, 1.0, 0.875, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.0, 0.875, 0.1250000000000001, 0.125, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.0, 0.0, 0.1250000000000001, 0.125, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.875, 0.0, 0.1250000000000001, 1.0, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.875, 0.875, 0.1250000000000001, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0625, 0.125, 0.9375, 0.09375, 0.875), BooleanOp.OR)


    val up = Shapes.empty()
        .join(Shapes.box(0.03125, 0.06249999999999989, 0.03125, 0.125, 0.8749999999999999, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.06249999999999989, 0.03125, 0.96875, 0.8749999999999999, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.8749999999999999, 0.125, 0.96875, 0.9374999999999999, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.06249999999999989, 0.875, 0.96875, 0.8749999999999999, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.875, 2.7755575615628914e-17, 0.125, 0.984375, 0.06250000000000003, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.8749999999999999, 0.125, 0.125, 0.9374999999999999, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.015625, 2.7755575615628914e-17, 0.125, 0.125, 0.06250000000000003, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.06249999999999989, 0.875, 0.125, 0.8749999999999999, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.8749999999999999, 0.03125, 0.875, 0.9374999999999999, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.8749999999999999, 0.875, 0.875, 0.9374999999999999, 0.96875), BooleanOp.OR).join(
            Shapes.box(0.015625, 2.7755575615628914e-17, 0.875, 0.984375, 0.06250000000000003, 0.984375), BooleanOp.OR
        ).join(
            Shapes.box(0.015625, 2.7755575615628914e-17, 0.015625, 0.984375, 0.06250000000000003, 0.125), BooleanOp.OR
        ).join(Shapes.box(0.0625, 0.0625, 0.125, 0.09375, 0.875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.90625, 0.0625, 0.125, 0.9375, 0.875, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0625, 0.90625, 0.875, 0.875, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.875, 0.125, 0.359375, 0.90625, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.875, 0.640625, 0.359375, 0.90625, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.875, 0.640625, 0.875, 0.90625, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.875, 0.125, 0.875, 0.90625, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.0, 0.125, 0.875, 0.03125, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0, 0.125, 0.359375, 0.03125, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0, 0.640625, 0.359375, 0.03125, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.0, 0.640625, 0.875, 0.03125, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.8749999999999999, 0.0, 0.125, 1.0, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.8749999999999999, 0.0, 1.0, 1.0, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.8749999999999999, 0.875, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.8749999999999999, 0.875, 0.125, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0625, 0.0625, 0.875, 0.875, 0.09375), BooleanOp.OR)


//north south east west up down


    val down = Shapes.empty()
        .join(Shapes.box(0.03125, 0.1250000000000001, 0.875, 0.125, 0.9375000000000001, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.1250000000000001, 0.875, 0.96875, 0.9375000000000001, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.06250000000000011, 0.125, 0.96875, 0.1250000000000001, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.1250000000000001, 0.03125, 0.96875, 0.9375000000000001, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.9375, 0.125, 0.984375, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.06250000000000011, 0.125, 0.125, 0.1250000000000001, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.9375, 0.125, 0.125, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.1250000000000001, 0.03125, 0.125, 0.9375000000000001, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.06250000000000011, 0.875, 0.875, 0.1250000000000001, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.06250000000000011, 0.03125, 0.875, 0.1250000000000001, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.9375, 0.015625, 0.984375, 1.0, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.9375, 0.875, 0.984375, 1.0, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.125, 0.125, 0.09375, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.90625, 0.125, 0.125, 0.9375, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.125, 0.0625, 0.875, 0.9375, 0.09375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.09375, 0.640625, 0.359375, 0.125, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.09375, 0.125, 0.359375, 0.125, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.09375, 0.125, 0.875, 0.125, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.09375, 0.640625, 0.875, 0.125, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.96875, 0.640625, 0.875, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.96875, 0.640625, 0.359375, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.96875, 0.125, 0.359375, 1.0, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.96875, 0.125, 0.875, 1.0, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.0, 0.875, 0.125, 0.1250000000000001, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.0, 0.875, 1.0, 0.1250000000000001, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.875, 0.0, 0.0, 1.0, 0.1250000000000001, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.0, 0.0, 0.0, 0.125, 0.1250000000000001, 0.125), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.125, 0.90625, 0.875, 0.9375, 0.9375), BooleanOp.OR)


    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(DirectionalBlock.FACING, pContext.clickedFace.opposite)
    }

    override fun rotate(pState: BlockState, pRotation: Rotation): BlockState {
        return pState.setValue(
            DirectionalBlock.FACING, pRotation.rotate(
                pState.getValue(
                    DirectionalBlock.FACING
                )
            )
        )
    }

    override fun mirror(pState: BlockState, pMirror: Mirror): BlockState {
        return pState.rotate(pMirror.getRotation(pState.getValue(DirectionalBlock.FACING)))
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(DirectionalBlock.FACING)
    }

    override fun getShape(
        pState: BlockState, pLevel: BlockGetter, pPos: BlockPos, pContext: CollisionContext
    ): VoxelShape = when (pState.getValue(DirectionalBlock.FACING)) {
        DOWN -> down
        UP -> up
        NORTH -> north
        SOUTH -> south
        WEST -> west
        EAST -> east
    }

    override fun getShadeBrightness(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos): Float {
        return 0.6f
    }

}