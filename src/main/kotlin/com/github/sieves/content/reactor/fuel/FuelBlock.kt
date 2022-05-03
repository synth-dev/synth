package com.github.sieves.content.reactor.fuel


import com.github.sieves.content.reactor.fuel.FuelBlock.FuelState.*
import com.github.sieves.dsl.*
import net.minecraft.core.*
import net.minecraft.util.*
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

class FuelBlock : Block(Properties.of(Material.HEAVY_METAL).noOcclusion()) {
    init {
        registerDefaultState(stateDefinition.any().setValue(State, UpDown))
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(State, UpDown)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(State)
    }

    override fun use(pState: BlockState, pLevel: Level, pPos: BlockPos, pPlayer: Player, pHand: InteractionHand, pHit: BlockHitResult): InteractionResult {

        val state = pState.setValue(State, FuelState.values()[(pState.getValue(State).ordinal + 1) % FuelState.values().size])
        pLevel.setBlockAndUpdate(pPos, state)
        return InteractionResult.sidedSuccess(pLevel.isClientSide)
    }

    private val upShape = Shapes.empty()
        .join(Shapes.box(0.0625, 0.9375, 0.0625, 0.9375, 1.0, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0, 0.125, 0.1875, 0.9375, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0, 0.8125, 0.1875, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.8125, 0.875, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.125, 0.875, 0.9375, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0, 0.15625, 0.8125, 0.9375, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0, 0.8125, 0.8125, 0.9375, 0.84375), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.1875, 0.84375, 0.9375, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.0, 0.1875, 0.1875, 0.9375, 0.8125), BooleanOp.OR)
    private val updownShape = Shapes.empty()
        .join(Shapes.box(0.0625, 0.9375, 0.0625, 0.9375, 1.0, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0625, 0.125, 0.1875, 0.9375, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0625, 0.8125, 0.1875, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0625, 0.8125, 0.875, 0.9375, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0625, 0.125, 0.875, 0.9375, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0625, 0.15625, 0.8125, 0.9375, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0625, 0.8125, 0.8125, 0.9375, 0.84375), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0625, 0.1875, 0.84375, 0.9375, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.0625, 0.1875, 0.1875, 0.9375, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.0625, 0.9375, 0.0625, 0.9375), BooleanOp.OR)

    private val downShape = Shapes.empty()
        .join(Shapes.box(0.125, 0.0625, 0.125, 0.1875, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0625, 0.8125, 0.1875, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0625, 0.8125, 0.875, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0625, 0.125, 0.875, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0625, 0.15625, 0.8125, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0625, 0.8125, 0.8125, 1.0, 0.84375), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0625, 0.1875, 0.84375, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.0625, 0.1875, 0.1875, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0, 0.0625, 0.9375, 0.0625, 0.9375), BooleanOp.OR)
    private val noneShape = Shapes.empty()
        .join(Shapes.box(0.125, 0.0, 0.125, 0.1875, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.0, 0.8125, 0.1875, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.8125, 0.875, 1.0, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.125, 0.875, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0, 0.15625, 0.8125, 1.0, 0.1875), BooleanOp.OR)
        .join(Shapes.box(0.1875, 0.0, 0.8125, 0.8125, 1.0, 0.84375), BooleanOp.OR)
        .join(Shapes.box(0.8125, 0.0, 0.1875, 0.84375, 1.0, 0.8125), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.0, 0.1875, 0.1875, 1.0, 0.8125), BooleanOp.OR)


    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    override fun getShape(
        pState: BlockState, pLevel: BlockGetter, pPos: BlockPos, pContext: CollisionContext
    ): VoxelShape {
        return when (pState.getValue(State)) {
            None -> noneShape
            UpDown -> updownShape
            Up -> upShape
            Down -> downShape
        }
    }


    companion object {
        val State: EnumProperty<FuelState> = EnumProperty.create("state", FuelState::class.java)
    }

    enum class FuelState : StringRepresentable {
        None, UpDown, Up, Down;

        override fun getSerializedName(): String {
            return this.name.lowercase()
        }
    }


}