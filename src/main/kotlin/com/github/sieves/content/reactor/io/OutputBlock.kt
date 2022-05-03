package com.github.sieves.content.reactor.io

import com.github.sieves.api.multiblock.*
import com.github.sieves.registry.Registry.Tiles
import com.github.sieves.dsl.*
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.phys.shapes.*

class OutputBlock : TileBlock<OutputTile>({ Tiles.Output }, 0) {

    init {
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, NORTH).setValue(Formed, false).setValue(Piped, false))
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, pContext.horizontalDirection)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(HorizontalDirectionalBlock.FACING).add(Formed).add(Piped)
    }

    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    override fun getShape(
        pState: BlockState, pLevel: BlockGetter, pPos: BlockPos, pContext: CollisionContext
    ): VoxelShape {
        val piped = pState.getValue(Piped)
        return if (pState.getValue(Formed)) {
            when (pState.getValue(HorizontalDirectionalBlock.FACING)) {
                NORTH -> if (!piped) northShape else qnorthShape
                SOUTH -> if (!piped) southShape else qsouthShape
                WEST -> if (!piped) westShape else qwestShape
                EAST -> if (!piped) eastShape else qeastShape
                else -> northShape
            }
        } else Shapes.block()
    }


    private val northShape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.07500000000000018, 0.4375, 0.8125, 0.20000000000000018, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.20000000000000018, 0.4375, 0.75, 0.2625000000000002, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.2625000000000002, 0.4375, 0.6875, 0.3250000000000002, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.3250000000000002, 0.4375, 0.625, 0.3875000000000002, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.3875000000000002, 0.4375, 0.5625, 0.4500000000000002, 0.5), BooleanOp.OR)

    private val westShape = Shapes.empty()
        .join(Shapes.box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.07500000000000018, 0.1875, 0.5, 0.20000000000000018, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.20000000000000018, 0.25, 0.5, 0.2625000000000002, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.2625000000000002, 0.3125, 0.5, 0.3250000000000002, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.3250000000000002, 0.375, 0.5, 0.3875000000000002, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.3875000000000002, 0.4375, 0.5, 0.4500000000000002, 0.5625), BooleanOp.OR)

    private val southShape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.07500000000000018, 0.5, 0.8125, 0.20000000000000018, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.20000000000000018, 0.5, 0.75, 0.2625000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.2625000000000002, 0.5, 0.6875, 0.3250000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.3250000000000002, 0.5, 0.625, 0.3875000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.3875000000000002, 0.5, 0.5625, 0.4500000000000002, 0.5625), BooleanOp.OR)

    private val eastShape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.07500000000000018, 0.1875, 0.5625, 0.20000000000000018, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.20000000000000018, 0.25, 0.5625, 0.2625000000000002, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.2625000000000002, 0.3125, 0.5625, 0.3250000000000002, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.3250000000000002, 0.375, 0.5625, 0.3875000000000002, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.3875000000000002, 0.4375, 0.5625, 0.4500000000000002, 0.5625), BooleanOp.OR)

    private val qnorthShape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.07500000000000018, 0.4375, 0.8125, 0.20000000000000018, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.20000000000000018, 0.4375, 0.75, 0.2625000000000002, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.2625000000000002, 0.4375, 0.6875, 0.3250000000000002, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.3250000000000002, 0.4375, 0.625, 0.3875000000000002, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.3875000000000002, 0.4375, 0.5625, 0.4500000000000002, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.36875, 0.7500000000000006, 0.321875, 0.63125, 0.7687500000000005, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.390625, 0.5375000000000005, 0.390625, 0.609375, 0.5625000000000006, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.60625, 0.5625000000000006, 0.328125, 0.625, 0.7500000000000006, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.5625000000000006, 0.33125000000000004, 0.39374999999999993, 0.7500000000000006, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.5937500000000006, -0.046875, 0.625, 0.6250000000000004, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.36875, 0.37500000000000056, -0.046875, 0.63125, 0.40625000000000056, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.59375, 0.40625000000000056, -0.046875, 0.625, 0.5937500000000006, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.3937500000000005, -0.06875000000000009, 0.6625, 0.6500000000000006, 0.006249999999999978), BooleanOp.OR)
        .join(
            Shapes.box(0.3375000000000008, 0.3937500000000005, -0.06875000000000009, 0.3750000000000009, 0.6500000000000006, 0.006249999999999978),
            BooleanOp.OR
        )
        .join(Shapes.box(0.375, 0.40625000000000056, -0.046875, 0.40625, 0.5937500000000006, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.3375, 0.3937499999999998, 0.006249999999999978, 0.6625000000000001, 0.6499999999999999, 0.03125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.5093749999999999, 0.34375, 0.640625, 0.765625, 0.40625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.4843749999999999, 0.28125, 0.640625, 0.753125, 0.34375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.45937499999999987, 0.21875, 0.640625, 0.7281249999999999, 0.28125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.43437499999999984, 0.15625, 0.640625, 0.6968749999999999, 0.21875), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.4093749999999998, 0.09375, 0.640625, 0.671875, 0.15625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.3843749999999998, 0.03125, 0.640625, 0.6468749999999999, 0.09375), BooleanOp.OR)
    private val qeastShape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.07500000000000018, 0.1875, 0.5625, 0.20000000000000018, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.20000000000000018, 0.25, 0.5625, 0.2625000000000002, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.2625000000000002, 0.3125, 0.5625, 0.3250000000000002, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.3250000000000002, 0.375, 0.5625, 0.3875000000000002, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.3875000000000002, 0.4375, 0.5625, 0.4500000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.7500000000000006, 0.36875, 0.678125, 0.7687500000000005, 0.63125), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.5375000000000005, 0.390625, 0.609375, 0.5625000000000006, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.5625000000000006, 0.60625, 0.671875, 0.7500000000000006, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.5625000000000006, 0.375, 0.66875, 0.7500000000000006, 0.39374999999999993), BooleanOp.OR)
        .join(Shapes.box(0.984375, 0.5937500000000006, 0.375, 1.046875, 0.6250000000000004, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.984375, 0.37500000000000056, 0.36875, 1.046875, 0.40625000000000056, 0.63125), BooleanOp.OR)
        .join(Shapes.box(0.984375, 0.40625000000000056, 0.59375, 1.046875, 0.5937500000000006, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.99375, 0.3937500000000005, 0.625, 1.06875, 0.6500000000000006, 0.6625), BooleanOp.OR)
        .join(Shapes.box(0.99375, 0.3937500000000005, 0.3375000000000008, 1.06875, 0.6500000000000006, 0.3750000000000009), BooleanOp.OR)
        .join(Shapes.box(0.984375, 0.40625000000000056, 0.375, 1.046875, 0.5937500000000006, 0.40625), BooleanOp.OR)
        .join(Shapes.box(0.96875, 0.3937499999999998, 0.3375, 0.99375, 0.6499999999999999, 0.6625000000000001), BooleanOp.OR)
        .join(Shapes.box(0.59375, 0.5093749999999999, 0.359375, 0.65625, 0.765625, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.65625, 0.4843749999999999, 0.359375, 0.71875, 0.753125, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.71875, 0.45937499999999987, 0.359375, 0.78125, 0.7281249999999999, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.78125, 0.43437499999999984, 0.359375, 0.84375, 0.6968749999999999, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.84375, 0.4093749999999998, 0.359375, 0.90625, 0.671875, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.90625, 0.3843749999999998, 0.359375, 0.96875, 0.6468749999999999, 0.640625), BooleanOp.OR)

    private val qsouthShape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.07500000000000018, 0.5, 0.8125, 0.20000000000000018, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.20000000000000018, 0.5, 0.75, 0.2625000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.2625000000000002, 0.5, 0.6875, 0.3250000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.3250000000000002, 0.5, 0.625, 0.3875000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.3875000000000002, 0.5, 0.5625, 0.4500000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.36875, 0.7500000000000006, 0.5, 0.63125, 0.7687500000000005, 0.678125), BooleanOp.OR)
        .join(Shapes.box(0.390625, 0.5375000000000005, 0.5, 0.609375, 0.5625000000000006, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.5625000000000006, 0.5, 0.39375000000000004, 0.7500000000000006, 0.671875), BooleanOp.OR)
        .join(Shapes.box(0.6062500000000001, 0.5625000000000006, 0.5, 0.625, 0.7500000000000006, 0.66875), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.5937500000000006, 0.984375, 0.625, 0.6250000000000004, 1.046875), BooleanOp.OR)
        .join(Shapes.box(0.36875, 0.37500000000000056, 0.984375, 0.63125, 0.40625000000000056, 1.046875), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.40625000000000056, 0.984375, 0.40625, 0.5937500000000006, 1.046875), BooleanOp.OR)
        .join(Shapes.box(0.3375, 0.3937500000000005, 0.99375, 0.375, 0.6500000000000006, 1.06875), BooleanOp.OR)
        .join(Shapes.box(0.6249999999999991, 0.3937500000000005, 0.99375, 0.6624999999999992, 0.6500000000000006, 1.06875), BooleanOp.OR)
        .join(Shapes.box(0.59375, 0.40625000000000056, 0.984375, 0.625, 0.5937500000000006, 1.046875), BooleanOp.OR)
        .join(Shapes.box(0.3374999999999999, 0.3937499999999998, 0.96875, 0.6625, 0.6499999999999999, 0.99375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.5093749999999999, 0.59375, 0.640625, 0.765625, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.4843749999999999, 0.65625, 0.640625, 0.753125, 0.71875), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.45937499999999987, 0.71875, 0.640625, 0.7281249999999999, 0.78125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.43437499999999984, 0.78125, 0.640625, 0.6968749999999999, 0.84375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.4093749999999998, 0.84375, 0.640625, 0.671875, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.3843749999999998, 0.90625, 0.640625, 0.6468749999999999, 0.96875), BooleanOp.OR)

    private val qwestShape = Shapes.empty()
        .join(Shapes.box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.07500000000000018, 0.1875, 0.5, 0.20000000000000018, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.20000000000000018, 0.25, 0.5, 0.2625000000000002, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.2625000000000002, 0.3125, 0.5, 0.3250000000000002, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.3250000000000002, 0.375, 0.5, 0.3875000000000002, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.3875000000000002, 0.4375, 0.5, 0.4500000000000002, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.321875, 0.7500000000000006, 0.36875, 0.5, 0.7687500000000005, 0.63125), BooleanOp.OR)
        .join(Shapes.box(0.390625, 0.5375000000000005, 0.390625, 0.5, 0.5625000000000006, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.328125, 0.5625000000000006, 0.375, 0.5, 0.7500000000000006, 0.39375000000000004), BooleanOp.OR)
        .join(Shapes.box(0.33125000000000004, 0.5625000000000006, 0.6062500000000001, 0.5, 0.7500000000000006, 0.625), BooleanOp.OR)
        .join(Shapes.box(-0.046875, 0.5937500000000006, 0.375, 0.015625, 0.6250000000000004, 0.625), BooleanOp.OR)
        .join(Shapes.box(-0.046875, 0.37500000000000056, 0.36875, 0.015625, 0.40625000000000056, 0.63125), BooleanOp.OR)
        .join(Shapes.box(-0.046875, 0.40625000000000056, 0.375, 0.015625, 0.5937500000000006, 0.40625), BooleanOp.OR)
        .join(Shapes.box(-0.06875000000000009, 0.3937500000000005, 0.3375, 0.006249999999999978, 0.6500000000000006, 0.375), BooleanOp.OR)
        .join(
            Shapes.box(-0.06875000000000009, 0.3937500000000005, 0.6249999999999991, 0.006249999999999978, 0.6500000000000006, 0.6624999999999992),
            BooleanOp.OR
        )
        .join(Shapes.box(-0.046875, 0.40625000000000056, 0.59375, 0.015625, 0.5937500000000006, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.006249999999999978, 0.3937499999999998, 0.3374999999999999, 0.03125, 0.6499999999999999, 0.6625), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.5093749999999999, 0.359375, 0.40625, 0.765625, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.28125, 0.4843749999999999, 0.359375, 0.34375, 0.753125, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.21875, 0.45937499999999987, 0.359375, 0.28125, 0.7281249999999999, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.43437499999999984, 0.359375, 0.21875, 0.6968749999999999, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.4093749999999998, 0.359375, 0.15625, 0.671875, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.3843749999999998, 0.359375, 0.09375, 0.6468749999999999, 0.640625), BooleanOp.OR)

    companion object {
        val Formed: BooleanProperty = BooleanProperty.create("formed")
        val Piped: BooleanProperty = BooleanProperty.create("piped")
    }

}