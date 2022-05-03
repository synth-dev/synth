package com.github.sieves.content.reactor.casing

import com.github.sieves.dsl.*
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.material.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*

class CasingBlock : Block(Properties.of(Material.HEAVY_METAL).noOcclusion()) {
    init {
        registerDefaultState(stateDefinition.any().setValue(DirectionalBlock.FACING, DOWN).setValue(Formed, false))
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(DirectionalBlock.FACING, pContext.clickedFace.opposite).setValue(Formed, false)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(DirectionalBlock.FACING).add(Formed)
    }

    /**
     * Form the multi-block
     */
    override fun use(pState: BlockState, level: Level, pPos: BlockPos, pPlayer: Player, pHand: InteractionHand, pHit: BlockHitResult): InteractionResult {
//        if (level.isClientSide) return InteractionResult.sidedSuccess(true)
//        level.setBlockAndUpdate(pPos, pState.setValue(Formed, !pState.getValue(Formed)))
//        return InteractionResult.sidedSuccess(false)
        return super.use(pState, level, pPos, pPlayer, pHand, pHit)
    }

    //Formed sides
    private val up = Shapes.empty()
        .join(Shapes.box(0.0, 0.5625, 0.0, 1.0, 1.0, 1.0), BooleanOp.OR)
    private val down = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 1.0, 0.4375, 1.0), BooleanOp.OR)
    private val east = Shapes.empty()
        .join(Shapes.box(0.5625, 0.0, 0.0, 1.0, 1.0, 1.0), BooleanOp.OR)
    private val west = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 0.4375, 1.0, 1.0), BooleanOp.OR)
    private val north = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.4375), BooleanOp.OR)
    private val south = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.5625, 1.0, 1.0, 1.0), BooleanOp.OR)


    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    override fun getShape(
        pState: BlockState, pLevel: BlockGetter, pPos: BlockPos, pContext: CollisionContext
    ): VoxelShape {
        return if (pState.getValue(Formed)) {
            when (pState.getValue(DirectionalBlock.FACING)) {
                DOWN -> up
                UP -> down
                NORTH -> north
                SOUTH -> south
                WEST -> west
                EAST -> east
            }
        } else Shapes.block()
    }


    companion object {
        val Formed: BooleanProperty = BooleanProperty.create("formed")
    }


}