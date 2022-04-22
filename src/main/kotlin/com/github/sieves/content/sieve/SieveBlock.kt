//package com.github.sieves.content.sieve
//
//import com.github.sieves.registry.Registry
//import com.github.sieves.registry.Registry.Net
//import com.github.sieves.registry.internal.net.ConfigurePacket
//import com.github.sieves.util.Log.info
//import com.github.sieves.util.getLevel
//import net.minecraft.core.BlockPos
//import net.minecraft.core.Direction
//import net.minecraft.server.level.ServerPlayer
//import net.minecraft.world.InteractionHand
//import net.minecraft.world.InteractionResult
//import net.minecraft.world.entity.player.Player
//import net.minecraft.world.item.context.BlockPlaceContext
//import net.minecraft.world.level.BlockGetter
//import net.minecraft.world.level.Level
//import net.minecraft.world.level.block.*
//import net.minecraft.world.level.block.entity.BlockEntity
//import net.minecraft.world.level.block.entity.BlockEntityTicker
//import net.minecraft.world.level.block.entity.BlockEntityType
//import net.minecraft.world.level.block.state.BlockState
//import net.minecraft.world.level.block.state.StateDefinition
//import net.minecraft.world.level.block.state.properties.DirectionProperty
//import net.minecraft.world.phys.BlockHitResult
//import net.minecraft.world.phys.shapes.BooleanOp
//import net.minecraft.world.phys.shapes.CollisionContext
//import net.minecraft.world.phys.shapes.Shapes
//import net.minecraft.world.phys.shapes.VoxelShape
//import net.minecraftforge.network.NetworkEvent
//
//
//class SieveBlock(properties: Properties) : Block(properties), EntityBlock {
//
//    private val northShape = north()
//    private val southShape = south()
//    private val eastShape = east()
//    private val westShape = west()
//
//    init {
//        Net.Configure.serverListener(::onConfiguration)
//    }
//
//    private fun onConfiguration(configurePacket: ConfigurePacket, context: NetworkEvent.Context): Boolean {
//        val level = configurePacket.getLevel(configurePacket.world)
//        val blockEntity = level.getBlockEntity(configurePacket.blockPos)
//        if (blockEntity !is SieveTile) return false
//        blockEntity.config.deserializeNBT(configurePacket.config.serializeNBT())
//        blockEntity.update()
//        info { "Updated configuration: $configurePacket" }
//        return true
//    }
//
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : BlockEntity?> getTicker(
//        pLevel: Level,
//        pState: BlockState,
//        pBlockEntityType: BlockEntityType<T>
//    ): BlockEntityTicker<T>? {
//        return if (!pLevel.isClientSide) SieveTile.Ticker as BlockEntityTicker<T> else null
//    }
//
//    override fun use(
//        pState: BlockState, pLevel: Level, pPos: BlockPos, pPlayer: Player, pHand: InteractionHand, pHit: BlockHitResult
//    ): InteractionResult {
//        if (!pLevel.isClientSide) {
//            val tile = pLevel.getBlockEntity(pPos)
//            if (tile is SieveTile) {
//                tile.openMenu(pPlayer as ServerPlayer)
//            }
//        }
//        return InteractionResult.SUCCESS
//    }
//
//    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
//        return defaultBlockState().setValue(FACING, pContext.horizontalDirection.opposite)
//    }
//
//    override fun rotate(pState: BlockState, pRotation: Rotation): BlockState {
//        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)))
//    }
//
//    override fun mirror(pState: BlockState, pMirror: Mirror): BlockState {
//        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)))
//    }
//
//    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
//        pBuilder.add(FACING)
//    }
//
//    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity? {
//        return Registry.Tiles.Sieve.create(pPos, pState)
//    }
//
//    override fun getShape(
//        pState: BlockState,
//        pLevel: BlockGetter,
//        pPos: BlockPos,
//        pContext: CollisionContext
//    ): VoxelShape {
//        return when (pState.getValue(FACING)) {
//            Direction.NORTH -> northShape
//            Direction.EAST -> eastShape
//            Direction.WEST -> westShape
//            Direction.SOUTH -> southShape
//            else -> northShape
//        }
//    }
//
//    override fun getShadeBrightness(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos): Float {
//        return 0.6f
//    }
//
//    private fun south(): VoxelShape {
//        var shape: VoxelShape = Shapes.empty()
//        shape = Shapes.join(shape, Shapes.box(0.375, 0.25, 0.84375, 0.625, 0.9375, 0.90625), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.8125, 0.0, 0.78125, 0.9375, 0.1875, 0.90625), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0, 0.03125, 0.1875, 0.1875, 0.15625), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.8125, 0.0, 0.03125, 0.9375, 0.1875, 0.15625), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0, 0.78125, 0.1875, 0.1875, 0.90625), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.03125, 0.9375, 0.25, 0.90625), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.875, 0.25, 0.03125, 0.9375, 0.9375, 0.90625), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.25, 0.03125, 0.125, 0.9375, 0.90625), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.125, 0.25, 0.03125, 0.875, 0.9375, 0.09375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.125, 0.25, 0.84375, 0.375, 0.9375, 0.96875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.625, 0.25, 0.84375, 0.875, 0.9375, 0.96875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.9375, 0.03125, 0.9375, 1.0, 0.90625), BooleanOp.OR)
//        return shape
//    }
//
//    private fun west(): VoxelShape {
//        var shape: VoxelShape = Shapes.empty()
//        shape = Shapes.join(shape, Shapes.box(0.078125, 0.25, 0.359375, 0.140625, 0.9375, 0.609375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.078125, 0.0, 0.796875, 0.203125, 0.1875, 0.921875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.828125, 0.0, 0.046875, 0.953125, 0.1875, 0.171875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.828125, 0.0, 0.796875, 0.953125, 0.1875, 0.921875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.078125, 0.0, 0.046875, 0.203125, 0.1875, 0.171875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.078125, 0.1875, 0.046875, 0.953125, 0.25, 0.921875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.078125, 0.25, 0.859375, 0.953125, 0.9375, 0.921875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.078125, 0.25, 0.046875, 0.953125, 0.9375, 0.109375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.890625, 0.25, 0.109375, 0.953125, 0.9375, 0.859375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.015625, 0.25, 0.109375, 0.140625, 0.9375, 0.359375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.015625, 0.25, 0.609375, 0.140625, 0.9375, 0.859375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.078125, 0.9375, 0.046875, 0.953125, 1.0, 0.921875), BooleanOp.OR)
//        return shape
//    }
//
//    private fun east(): VoxelShape {
//        var shape: VoxelShape = Shapes.empty()
//        shape = Shapes.join(shape, Shapes.box(0.859375, 0.25, 0.359375, 0.921875, 0.9375, 0.609375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.796875, 0.0, 0.046875, 0.921875, 0.1875, 0.171875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.046875, 0.0, 0.796875, 0.171875, 0.1875, 0.921875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.046875, 0.0, 0.046875, 0.171875, 0.1875, 0.171875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.796875, 0.0, 0.796875, 0.921875, 0.1875, 0.921875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.046875, 0.1875, 0.046875, 0.921875, 0.25, 0.921875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.046875, 0.25, 0.046875, 0.921875, 0.9375, 0.109375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.046875, 0.25, 0.859375, 0.921875, 0.9375, 0.921875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.046875, 0.25, 0.109375, 0.109375, 0.9375, 0.859375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.859375, 0.25, 0.609375, 0.984375, 0.9375, 0.859375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.859375, 0.25, 0.109375, 0.984375, 0.9375, 0.359375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.046875, 0.9375, 0.046875, 0.921875, 1.0, 0.921875), BooleanOp.OR)
//        return shape
//    }
//
//    fun north(): VoxelShape {
//        var shape: VoxelShape = Shapes.empty()
//        shape = Shapes.join(shape, Shapes.box(0.375, 0.25, 0.0625, 0.625, 0.9375, 0.125), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0, 0.0625, 0.1875, 0.1875, 0.1875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.8125, 0.0, 0.8125, 0.9375, 0.1875, 0.9375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0, 0.8125, 0.1875, 0.1875, 0.9375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.8125, 0.0, 0.0625, 0.9375, 0.1875, 0.1875), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.0625, 0.9375, 0.25, 0.9375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.25, 0.0625, 0.125, 0.9375, 0.9375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.875, 0.25, 0.0625, 0.9375, 0.9375, 0.9375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.125, 0.25, 0.875, 0.875, 0.9375, 0.9375), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.625, 0.25, 0.0, 0.875, 0.9375, 0.125), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.125, 0.25, 0.0, 0.375, 0.9375, 0.125), BooleanOp.OR)
//        shape = Shapes.join(shape, Shapes.box(0.0625, 0.9375, 0.0625, 0.9375, 1.0, 0.9375), BooleanOp.OR)
//        return shape
//    }
//
//    companion object {
//        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
//    }
//}