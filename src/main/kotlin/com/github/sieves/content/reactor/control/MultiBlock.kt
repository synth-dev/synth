package com.github.sieves.content.reactor.control

import com.github.sieves.api.tile.*
import com.github.sieves.content.io.link.*
import com.github.sieves.content.reactor.casing.*
import com.github.sieves.content.reactor.casing.PanelBlock.PanelState.*
import com.github.sieves.content.reactor.io.*
import com.github.sieves.registry.Registry.Blocks
import com.github.sieves.util.*
import com.github.sieves.util.Log.debug
import com.github.sieves.util.Log.info
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.*
import net.minecraft.core.Vec3i
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.util.INBTSerializable

/**
 * Stores the multi-block information
 */
data class MultiBlock(val center: BlockPos) : INBTSerializable<CompoundTag> {
    var isFormed: Boolean = false
    internal val links = Links()
    private val processed = HashMap<BlockPos, BlockState>()
    var min: Vec3i = Vec3i.ZERO.min()
        private set
    var max: Vec3i = Vec3i.ZERO.max()
        private set

    //Gets the min/max bounds and creates an aabb
    val aabb: AABB
        get() {
            updateBounds()
            return AABB(min.bp, max.bp)
        }


    /**
     * Forms the multi-block, by searching for the correct blocks
     * [forward] is the clicked face upon trying to form the multi block. It's needed
     * to walk the neighboring blocks
     */
    fun form(level: Level, forward: Direction) {
        if (isFormed) return
        //Reset all the state
//        for (link in links.getLinks().keys) {
//            val state = level.getBlockState(link).block.defaultBlockState()
//            level.setBlockAndUpdate(link, state)
//        }
        links.removeLinks()
        processed.clear()
        checkAddNeighbors(level, center, forward)
        if (construct(level)) {
            for ((key, value) in processed) {
                level.setBlockAndUpdate(key, value)
                val be = level.getBlockEntity(key)
                if (be !is IMultiBlock<*>) continue
                be.ctrlPos = (center)

            }
            isFormed = true
        } else {
            links.removeLinks()
            processed.clear()
            isFormed = false
        }
    }

    /**
     * This will check all the blocks to see if they're still valid
     */
    fun checkBlocks(level: Level): Boolean {
        for (block in links.getLinks().keys) {
            if (level.getBlockState(block).isAir) {
                unform(level)
                return false
            }
        }
        return true
    }

    /**
     * Called upon the removal of a block within our radius
     */
    fun unform(level: Level) {
        for (block in links.getLinks().keys) {
            val state = level.getBlockState(block).block.defaultBlockState()
            level.setBlockAndUpdate(block, state)
        }
        links.removeLinks()
        processed.clear()
        isFormed = false
    }

    /**
     * This will take the blockPos at the given position at the given position, and
     * find all neighbors that are correct
     */
    private fun checkAddNeighbors(level: Level, blockPos: BlockPos, forward: Direction) {
        val state = level.getBlockState(blockPos)
        //Break when we hit air, no side checks here
        if (links.getLinks().containsKey(blockPos) || processed.containsKey(blockPos)) return
        processed[blockPos] = state
        if (state.`is`(Blocks.Control)) {
            processed[blockPos] =
                Blocks.Control.defaultBlockState().setValue(ControlBlock.Formed, true).setValue(HorizontalDirectionalBlock.FACING, forward.opposite)
            links.addLink(blockPos, forward)
        }

        //get the up/down/left/right offsets for the direction,
        //Iterate through each blockpos offset check to see if it's valid
        val up = level.getBlockState(blockPos.offset(UP))
        if ((up.`is`(Blocks.Panel) || up.`is`(Blocks.Case) || up.`is`(Blocks.Input) || up.`is`(Blocks.Output)) && !processed.containsKey(blockPos.offset(UP))) checkAddNeighbors(
            level, blockPos.offset(UP), forward
        )
        val down = level.getBlockState(blockPos.offset(DOWN))
        if ((down.`is`(Blocks.Panel) || down.`is`(Blocks.Case) || down.`is`(Blocks.Input) || down.`is`(Blocks.Output)) && !processed.containsKey(
                blockPos.offset(
                    DOWN
                )
            )) checkAddNeighbors(
            level, blockPos.offset(DOWN), forward
        )
        val left = level.getBlockState(blockPos.offset(forward.clockWise))
        if ((left.`is`(Blocks.Panel) || left.`is`(Blocks.Case) || left.`is`(Blocks.Input) || left.`is`(Blocks.Output)) && !processed.containsKey(
                blockPos.offset(
                    forward.clockWise
                )
            )) checkAddNeighbors(
            level, blockPos.offset(forward.clockWise), forward
        )
        val right = level.getBlockState(blockPos.offset(forward.counterClockWise))

        val back = level.getBlockState(blockPos.offset(forward))
        if ((back.`is`(Blocks.Panel) || back.`is`(Blocks.Case) && left.isAir) && !processed.containsKey(blockPos.offset(forward))) {
            checkAddNeighbors(level, blockPos.offset(forward), forward.counterClockWise)
        }

        val front = level.getBlockState(blockPos.offset(forward.opposite))
        if (state.`is`(Blocks.Panel)) {

            if (up.isAir && !left.isAir && !right.isAir && (front.isAir || back.isAir)) {
                processed[blockPos] = state.setValue(PanelBlock.State, HEdge).setValue(DirectionalBlock.FACING, forward)

            } else if (left.isAir && !up.isAir) {
                processed[blockPos] = state.setValue(PanelBlock.State, VEdge).setValue(DirectionalBlock.FACING, forward.counterClockWise)
            } else if (up.isAir && left.isAir) {
                processed[blockPos] = state.setValue(PanelBlock.State, Corner).setValue(DirectionalBlock.FACING, forward.counterClockWise)
            } else if (front.isAir && !up.isAir && !down.isAir && !left.isAir) {
                processed[blockPos] = state.setValue(PanelBlock.State, Side).setValue(DirectionalBlock.FACING, forward)
            } else {
                if (down.isAir || down.`is`(Blocks.Spark)) {
                    processed[blockPos] = state.setValue(PanelBlock.State, Side).setValue(DirectionalBlock.FACING, DOWN)
                }
            }
            links.addLink(blockPos, forward)
        } else if (state.`is`(Blocks.Case)) {
            processed[blockPos] = state.setValue(DirectionalBlock.FACING, forward).setValue(CasingBlock.Formed, true)
            links.addLink(blockPos, forward)
        } else if (state.`is`(Blocks.Input)) {
            processed[blockPos] =
                state.setValue(HorizontalDirectionalBlock.FACING, forward.opposite).setValue(InputBlock.Formed, true).setValue(InputBlock.Piped, false)
            links.addLink(blockPos, forward)
        } else if (state.`is`(Blocks.Output)) {
            processed[blockPos] =
                state.setValue(HorizontalDirectionalBlock.FACING, forward.opposite).setValue(OutputBlock.Formed, true).setValue(OutputBlock.Piped, false)
            links.addLink(blockPos, forward)
        }
    }

    /***
     * Used to iterate through the insides of the tile entity
     */
    internal inline fun iterateInside(iterator: (pos: BlockPos) -> Unit) {
//        updateBounds()
//        for (x in min.x..max.x) for (y in min.y..max.y) for (z in min.z..max.z) {
//            iterator(BlockPos(x, y, z))
//        }
    }

    fun updateBounds() {

    }

    /**
     * Validates all the blocks within the range
     */
    private fun construct(level: Level): Boolean {
        this.min = Vec3i(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        this.max = Vec3i(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
        for (block in links) {
            if (block.x >= max.x && block.y >= max.y && block.z >= max.z) max = block
            else if (block.x <= min.x && block.y <= min.y && block.z <= min.z) min = block
        }
        val dx = max.x - min.x
        val dy = max.y - min.y
        val dz = max.z - min.z
        if (dx < 2 || dx > 48 || dy < 2 || dy > 48 || dz < 2 || dz > 48) return false
        for (y in 0 until dy) {
            val northEast = level.getBlockState(BlockPos(max.x, max.y - y, max.z))
            val northWest = level.getBlockState(BlockPos(max.x, max.y - y, min.z))
            val southWest = level.getBlockState(BlockPos(min.x, max.y - y, min.z))
            val southEast = level.getBlockState(BlockPos(min.x, max.y - y, max.z))
            debug { "${northEast.block.name.string}, ${northWest.block.name.string}, ${southWest.block.name.string}, ${southEast.block.name.string}" }

            if (!northEast.`is`(Blocks.Panel) || !northWest.`is`(Blocks.Panel) || !southWest.`is`(Blocks.Panel) || !southEast.`is`(Blocks.Panel)) {
                return false
            }
        }

        debug { "$dx, $dy, $dz" }
        var hasController = false
        var hasInput = false
        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                for (z in min.z..max.z) {
                    if (y == min.y) {
                        val below = level.getBlockState(BlockPos(x, y - 1, z))
                        if (below.isAir) return false
                    }
                    if (x == min.x || z == min.z || x == max.x || z == max.z) {
                        val state = level.getBlockState(BlockPos(x, y, z))
                        if (!state.`is`(Blocks.Case) && !state.`is`(Blocks.Panel) && !state.`is`(Blocks.Control) && !state.`is`(Blocks.Input) && !state.`is`(
                                Blocks.Output
                            )) return false
                        if (state.`is`(Blocks.Control)) {
                            if (hasController) return false
                            else hasController = true
                        }
                        if (state.`is`(Blocks.Input)) {
                            if (hasInput) return false
                            else hasInput = true
                        }
                        info { "iterating[${state.block.name.string}]: $x, $y, $z" }
                    }
                }
            }
        }

        return hasController && hasInput
    }


    override fun serializeNBT(): CompoundTag {
        val compound = CompoundTag()
        compound.put("links", links.serializeNBT())
        compound.putBoolean("formed", isFormed)
        compound.putBlockPos("min", this.min)
        compound.putBlockPos("max", this.max)
        return compound
    }

    override fun deserializeNBT(nbt: CompoundTag) {
        this.links.deserializeNBT(nbt.getCompound("links"))
        this.isFormed = nbt.getBoolean("formed")
        this.min = nbt.getBlockPos("min")
        this.max = nbt.getBlockPos("max")
    }

}