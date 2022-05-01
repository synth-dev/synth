package com.github.sieves.content.reactor.control

import com.github.sieves.api.multiblock.*
import com.github.sieves.api.multiblock.StructureBlockVariant.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.reactor.casing.*
import com.github.sieves.content.reactor.io.*
import com.github.sieves.registry.Registry.Blocks
import com.github.sieves.registry.Registry.Tiles
import com.github.sieves.util.*
import com.github.sieves.util.Log.info
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.*
import net.minecraft.nbt.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.InteractionResult.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.*
import net.minecraft.world.level.block.Blocks as McBlocks

/**
 * The "brain" of the reactor. Is responsible for updating the states of the casings, finding input/outputs/and sparks
 */
class ControlTile(blockPos: BlockPos, blockState: BlockState) : BaseTile<ControlTile>(Tiles.Control, blockPos, blockState), IMaster<ControlTile> {
    /**Keep track of our structure**/
    private var structure: Opt<StructureStore> = Opt.nil()

    /**Keep track of tick to do logic at fixed rate**/
    private var tick = 0


    /**
     * Called right before the first tick, add initialization logic here if it requires the world to be present
     */
    override fun onInit() = world.ifPresent {
        updateSlaves(structure)
    }

    /**
     * Delegate ticking of the server here
     */
    override fun onTick(level: Level) {
        if (level.isClientSide) return
        if (tick >= 20) {
            if (structure.isPresent && !isValid(structure)) {
                unform(structure)
                info { "Unformed structure because it was invalid: $structure" }
                structure = Opt.nil()
            }
            tick = 0
        }
        tick++
    }

    /**
     * Attempt to form our multi block here, if valid we store the multiblock
     */
    override fun onUse(level: Level, player: Player, itemUsed: ItemStack, direction: Direction): InteractionResult {
        if (level.isClientSide) return SUCCESS
        if (player.isShiftKeyDown) {
            unform(structure)
            structure = Opt.nil()
            return CONSUME
        }
        if (this.structure.isPresent) return PASS
        this.structure = form(direction.opposite)
        structure.ifPresent {
            info { "Formed structure: $structure" }
        }
        return CONSUME
    }

    /**
     * Called upon destroying the block, we want to update our multiblock in this case to destroy it
     */
    override fun onBreak(level: Level, player: Player, willHarvest: Boolean, fluid: FluidState) = unform(structure)

    /**
     * Required to check to see if a block at the given position and type is valid
     */
    override fun isValidVariant(blockPos: BlockPos, blockState: BlockState, type: StructureBlockVariant, structure: Opt<StructureStore>): Boolean =
        when (type) {
            Side -> blockState.`is`(Blocks.Panel) || blockState.`is`(Blocks.Control) || blockState.`is`(Blocks.Input) || blockState.`is`(Blocks.Output) || blockState.`is`(
                Blocks.Case
            )
            VerticalEdge -> blockState.`is`(Blocks.Panel)
            HorizontalEdge -> blockState.`is`(Blocks.Panel)
            Inner -> blockState.`is`(McBlocks.AIR) || blockState.`is`(Blocks.Spark) || blockState.`is`(Blocks.Fuel)
            Corner -> blockState.`is`(Blocks.Panel)
        }

    /**
     * Used to update the state of a given block. By default, it just returns the current state,
     * use this in the override version to just get the default state and update it
     */
    override fun setFormedAt(blockPos: BlockPos, direction: Direction, variant: StructureBlockVariant, store: StructureStore): BlockState {
        var state = super.setFormedAt(blockPos, direction, variant, store)
        when (state.block) {
            is PanelBlock -> {
                state =
                    if (blockPos.y == store.max.y && blockPos.x != store.max.x && blockPos.x != store.min.x && blockPos.z != store.min.z && blockPos.z != store.max.z) state.setValue(
                        DirectionalBlock.FACING,
                        DOWN
                    )
                    else state.setValue(DirectionalBlock.FACING, direction)
                if (blockPos.y > store.min.y) { //don't set state for bottom panel
                    state = state.setValue(PanelBlock.State, variant.panelState)
                }
            }
            is CasingBlock -> state = state.setValue(DirectionalBlock.FACING, direction).setValue(CasingBlock.Formed, true)
            is ControlBlock -> state = state.setValue(HorizontalDirectionalBlock.FACING, direction.horizontal.opposite).setValue(ControlBlock.Formed, true)
            is OutputBlock -> state = state.setValue(HorizontalDirectionalBlock.FACING, direction.horizontal.opposite).setValue(OutputBlock.Formed, true)
            is InputBlock -> state = state.setValue(HorizontalDirectionalBlock.FACING, direction.horizontal.opposite).setValue(InputBlock.Formed, true)
        }
        return state
    }


    /**
     * Save our structure
     */
    override fun onSave(tag: CompoundTag) {
        structure.ifPresent {
            tag.put("structure", it.serializeNBT())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onLoad(tag: CompoundTag) {
        if (tag.contains("structure")) {
            val structureTag = tag.getCompound("structure")
            this.structure = Opt.of(StructureStore(structureTag))
        }
    }


}