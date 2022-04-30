package com.github.sieves.content.reactor.io

import com.github.sieves.api.tile.*
import com.github.sieves.content.reactor.casing.*
import com.github.sieves.registry.Registry.Tiles
import com.github.sieves.util.*
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.material.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*

class InputBlock : Block(Properties.of(Material.HEAVY_METAL).noOcclusion()), EntityBlock {
    init {
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, NORTH).setValue(Formed, false).setValue(Piped, false))
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, pContext.horizontalDirection).setValue(Formed, false).setValue(Piped, false)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(HorizontalDirectionalBlock.FACING).add(Formed).add(Piped)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity?> getTicker(pLevel: Level, pState: BlockState, pBlockEntityType: BlockEntityType<T>): BlockEntityTicker<T> =
        ReactorTile.Ticker as BlockEntityTicker<T>

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity? = Tiles.Input.create(pPos, pState)

    override fun onDestroyedByPlayer(state: BlockState?, level: Level?, pos: BlockPos?, player: Player?, willHarvest: Boolean, fluid: FluidState?): Boolean {
        val tile = pos?.let { level?.getBlockEntity(it) } ?: return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
        if (tile !is IMultiBlock<*>) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
        player?.let { fluid?.let { it1 -> tile.onDestroy(it, willHarvest, it1) } }
        return super.onDestroyedByPlayer(
            state, level, pos, player, willHarvest, fluid
        )
    }

//    override fun use(pState: BlockState, pLevel: Level, pPos: BlockPos, pPlayer: Player, pHand: InteractionHand, pHit: BlockHitResult): InteractionResult {
//        val tile = pLevel.getBlockEntity(pPos)
//        if (tile !is IMultiBlock<*>) return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit)
//        return if (pLevel.isClientSide) tile.onUseClient(pPlayer, pHand, pHit)
//        else tile.onUseServer(pPlayer as ServerPlayer, pHand, pHit)
//    }


    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    override fun getShape(
        pState: BlockState, pLevel: BlockGetter, pPos: BlockPos, pContext: CollisionContext
    ): VoxelShape {
        val piped = pState.getValue(Piped)
        return if (pState.getValue(CasingBlock.Formed)) {
            when (pState.getValue(HorizontalDirectionalBlock.FACING)) {
                NORTH -> if (!piped) north else pnorth
                SOUTH -> if (!piped) south else psouth
                WEST -> if (!piped) west else pwest
                EAST -> if (!piped) east else peast
                else -> north
            }
        } else Shapes.block()
    }

    private val pnorth = Shapes.empty().join(Shapes.box(0.1875, 0.7999999999999998, 0.4375, 0.8125, 0.9249999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.7374999999999998, 0.4375, 0.75, 0.7999999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.6749999999999998, 0.4375, 0.6875, 0.7374999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.6124999999999998, 0.4375, 0.625, 0.6749999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.5499999999999998, 0.4375, 0.5625, 0.6124999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.36875, 0.23124999999999957, 0.321875, 0.63125, 0.24999999999999944, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.390625, 0.43749999999999944, 0.390625, 0.609375, 0.46249999999999947, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.24999999999999944, 0.328125, 0.39375000000000004, 0.43749999999999944, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.6062500000000001, 0.24999999999999944, 0.33125000000000004, 0.625, 0.43749999999999944, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.37499999999999956, -0.046875, 0.625, 0.40624999999999944, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.36875, 0.5937499999999994, -0.046875, 0.63125, 0.6249999999999994, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.40624999999999944, -0.046875, 0.40625, 0.5937499999999994, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.3375, 0.3499999999999995, -0.06875000000000009, 0.375, 0.6062499999999995, 0.006249999999999978), BooleanOp.OR)
        .join(
            Shapes.box(0.6249999999999991, 0.3499999999999995, -0.06875000000000009, 0.6624999999999992, 0.6062499999999995, 0.006249999999999978),
            BooleanOp.OR
        )
        .join(Shapes.box(0.59375, 0.40624999999999944, -0.046875, 0.625, 0.5937499999999994, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.234375, 0.34375, 0.640625, 0.49062500000000003, 0.40625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.246875, 0.28125, 0.640625, 0.5156250000000001, 0.34375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.27187500000000003, 0.21875, 0.640625, 0.5406250000000001, 0.28125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.30312500000000003, 0.15625, 0.640625, 0.5656250000000002, 0.21875), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.32812500000000006, 0.09375, 0.640625, 0.5906250000000002, 0.15625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.3531250000000001, 0.03125, 0.640625, 0.6156250000000002, 0.09375), BooleanOp.OR)
        .join(Shapes.box(0.33749999999999997, 0.3500000000000001, 0.006249999999999988, 0.6625, 0.6062500000000002, 0.03125), BooleanOp.OR)

    private val pwest = Shapes.empty().join(Shapes.box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.7999999999999998, 0.1875, 0.5, 0.9249999999999998, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.7374999999999998, 0.25, 0.5, 0.7999999999999998, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.6749999999999998, 0.3125, 0.5, 0.7374999999999998, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.6124999999999998, 0.375, 0.5, 0.6749999999999998, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.5499999999999998, 0.4375, 0.5, 0.6124999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.321875, 0.23124999999999957, 0.36875, 0.5, 0.24999999999999944, 0.63125), BooleanOp.OR)
        .join(Shapes.box(0.390625, 0.43749999999999944, 0.390625, 0.5, 0.46249999999999947, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.328125, 0.24999999999999944, 0.60625, 0.5, 0.43749999999999944, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.33125000000000004, 0.24999999999999944, 0.375, 0.5, 0.43749999999999944, 0.39374999999999993), BooleanOp.OR)
        .join(Shapes.box(-0.046875, 0.37499999999999956, 0.375, 0.015625, 0.40624999999999944, 0.625), BooleanOp.OR)
        .join(Shapes.box(-0.046875, 0.5937499999999994, 0.36875, 0.015625, 0.6249999999999994, 0.63125), BooleanOp.OR)
        .join(Shapes.box(-0.046875, 0.40624999999999944, 0.59375, 0.015625, 0.5937499999999994, 0.625), BooleanOp.OR)
        .join(Shapes.box(-0.06875000000000009, 0.3499999999999995, 0.625, 0.006249999999999978, 0.6062499999999995, 0.6625), BooleanOp.OR)
        .join(
            Shapes.box(-0.06875000000000009, 0.3499999999999995, 0.3375000000000008, 0.006249999999999978, 0.6062499999999995, 0.3750000000000009),
            BooleanOp.OR
        )
        .join(Shapes.box(-0.046875, 0.40624999999999944, 0.375, 0.015625, 0.5937499999999994, 0.40625), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.234375, 0.359375, 0.40625, 0.49062500000000003, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.28125, 0.246875, 0.359375, 0.34375, 0.5156250000000001, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.21875, 0.27187500000000003, 0.359375, 0.28125, 0.5406250000000001, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.30312500000000003, 0.359375, 0.21875, 0.5656250000000002, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.09375, 0.32812500000000006, 0.359375, 0.15625, 0.5906250000000002, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.03125, 0.3531250000000001, 0.359375, 0.09375, 0.6156250000000002, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.006249999999999978, 0.3500000000000001, 0.3375, 0.03125, 0.6062500000000002, 0.6625000000000001), BooleanOp.OR)

    private val peast = Shapes.empty().join(Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.7999999999999998, 0.1875, 0.5625, 0.9249999999999998, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.7374999999999998, 0.25, 0.5625, 0.7999999999999998, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.6749999999999998, 0.3125, 0.5625, 0.7374999999999998, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.6124999999999998, 0.375, 0.5625, 0.6749999999999998, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.5499999999999998, 0.4375, 0.5625, 0.6124999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.23124999999999957, 0.36875, 0.678125, 0.24999999999999944, 0.63125), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.43749999999999944, 0.390625, 0.609375, 0.46249999999999947, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.24999999999999944, 0.375, 0.671875, 0.43749999999999944, 0.39375000000000004), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.24999999999999944, 0.6062500000000001, 0.66875, 0.43749999999999944, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.984375, 0.37499999999999956, 0.375, 1.046875, 0.40624999999999944, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.984375, 0.5937499999999994, 0.36875, 1.046875, 0.6249999999999994, 0.63125), BooleanOp.OR)
        .join(Shapes.box(0.984375, 0.40624999999999944, 0.375, 1.046875, 0.5937499999999994, 0.40625), BooleanOp.OR)
        .join(Shapes.box(0.99375, 0.3499999999999995, 0.3375, 1.06875, 0.6062499999999995, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.99375, 0.3499999999999995, 0.6249999999999991, 1.06875, 0.6062499999999995, 0.6624999999999992), BooleanOp.OR)
        .join(Shapes.box(0.984375, 0.40624999999999944, 0.59375, 1.046875, 0.5937499999999994, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.59375, 0.234375, 0.359375, 0.65625, 0.49062500000000003, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.65625, 0.246875, 0.359375, 0.71875, 0.5156250000000001, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.71875, 0.27187500000000003, 0.359375, 0.78125, 0.5406250000000001, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.78125, 0.30312500000000003, 0.359375, 0.84375, 0.5656250000000002, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.84375, 0.32812500000000006, 0.359375, 0.90625, 0.5906250000000002, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.90625, 0.3531250000000001, 0.359375, 0.96875, 0.6156250000000002, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.96875, 0.3500000000000001, 0.33749999999999997, 0.99375, 0.6062500000000002, 0.6625), BooleanOp.OR)
    private val psouth = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.7999999999999998, 0.5, 0.8125, 0.9249999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.7374999999999998, 0.5, 0.75, 0.7999999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.6749999999999998, 0.5, 0.6875, 0.7374999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.6124999999999998, 0.5, 0.625, 0.6749999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.5499999999999998, 0.5, 0.5625, 0.6124999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.7999999999999998, 0.5, 0.8125, 0.9249999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.7374999999999998, 0.5, 0.75, 0.7999999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.6749999999999998, 0.5, 0.6875, 0.7374999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.6124999999999998, 0.5, 0.625, 0.6749999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.5499999999999998, 0.5, 0.5625, 0.6124999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.36875, 0.23124999999999957, 0.5, 0.63125, 0.24999999999999944, 0.678125), BooleanOp.OR)
        .join(Shapes.box(0.390625, 0.43749999999999944, 0.5, 0.609375, 0.46249999999999947, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.60625, 0.24999999999999944, 0.5, 0.625, 0.43749999999999944, 0.671875), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.24999999999999944, 0.5, 0.39374999999999993, 0.43749999999999944, 0.66875), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.37499999999999956, 0.984375, 0.625, 0.40624999999999944, 1.046875), BooleanOp.OR)
        .join(Shapes.box(0.36875, 0.5937499999999994, 0.984375, 0.63125, 0.6249999999999994, 1.046875), BooleanOp.OR)
        .join(Shapes.box(0.59375, 0.40624999999999944, 0.984375, 0.625, 0.5937499999999994, 1.046875), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.3499999999999995, 0.99375, 0.6625, 0.6062499999999995, 1.06875), BooleanOp.OR)
        .join(Shapes.box(0.3375000000000008, 0.3499999999999995, 0.99375, 0.3750000000000009, 0.6062499999999995, 1.06875), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.40624999999999944, 0.984375, 0.40625, 0.5937499999999994, 1.046875), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.234375, 0.59375, 0.640625, 0.49062500000000003, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.246875, 0.65625, 0.640625, 0.5156250000000001, 0.71875), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.27187500000000003, 0.71875, 0.640625, 0.5406250000000001, 0.78125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.30312500000000003, 0.78125, 0.640625, 0.5656250000000002, 0.84375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.32812500000000006, 0.84375, 0.640625, 0.5906250000000002, 0.90625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.3531250000000001, 0.90625, 0.640625, 0.6156250000000002, 0.96875), BooleanOp.OR)
        .join(Shapes.box(0.3375, 0.3500000000000001, 0.96875, 0.6625000000000001, 0.6062500000000002, 0.99375), BooleanOp.OR)


    companion object {
        val Formed: BooleanProperty = BooleanProperty.create("formed")
        val Piped: BooleanProperty = BooleanProperty.create("piped")
    }

    private val east = Shapes.empty().join(Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.7999999999999998, 0.1875, 0.5625, 0.9249999999999998, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.7374999999999998, 0.25, 0.5625, 0.7999999999999998, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.6749999999999998, 0.3125, 0.5625, 0.7374999999999998, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.6124999999999998, 0.375, 0.5625, 0.6749999999999998, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.5, 0.5499999999999998, 0.4375, 0.5625, 0.6124999999999998, 0.5625), BooleanOp.OR)
    private val north = Shapes.empty().join(Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.7999999999999998, 0.4375, 0.8125, 0.9249999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.7374999999999998, 0.4375, 0.75, 0.7999999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.6749999999999998, 0.4375, 0.6875, 0.7374999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.6124999999999998, 0.4375, 0.625, 0.6749999999999998, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.5499999999999998, 0.4375, 0.5625, 0.6124999999999998, 0.5), BooleanOp.OR)
    private val south = Shapes.empty().join(Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.5), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.7999999999999998, 0.5, 0.8125, 0.9249999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.25, 0.7374999999999998, 0.5, 0.75, 0.7999999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.3125, 0.6749999999999998, 0.5, 0.6875, 0.7374999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.375, 0.6124999999999998, 0.5, 0.625, 0.6749999999999998, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.5499999999999998, 0.5, 0.5625, 0.6124999999999998, 0.5625), BooleanOp.OR)
    private val west = Shapes.empty().join(Shapes.box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.7999999999999998, 0.1875, 0.5, 0.9249999999999998, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.7374999999999998, 0.25, 0.5, 0.7999999999999998, 0.75), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.6749999999999998, 0.3125, 0.5, 0.7374999999999998, 0.6875), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.6124999999999998, 0.375, 0.5, 0.6749999999999998, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.4375, 0.5499999999999998, 0.4375, 0.5, 0.6124999999999998, 0.5625), BooleanOp.OR)


}