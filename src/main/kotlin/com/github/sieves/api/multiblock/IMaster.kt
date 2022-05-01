package com.github.sieves.api.multiblock

import com.github.sieves.api.multiblock.StructureBlockVariant.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.util.*
import com.github.sieves.util.Log.debug
import com.github.sieves.util.Log.error
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*

interface IMaster<T : BlockEntity> : ITile<T> {

    /**
     * This will compute the boundaries for the multiblock structure
     */
    fun computeStructure(forward: Direction): Opt<MultiBlockStructure> {
        val bounds = locateBounds(forward)
        if (bounds.isAbsent) return Opt.nil()
        return Opt.of(MultiBlockStructure(bounds().first, bounds().second))
    }

    /**
     * Computes the min/max for this master block by iterating through until
     * we find an edge
     */
    private fun locateBounds(forward: Direction): Opt<Pair<Vec3i, Vec3i>> {
        if (world.isAbsent) return Opt.nil()
        //Will compute the bottom left position for the given pos/direction
        val min = locateMinimum(this.pos, forward)
        //return when invalid
        if (min.isAbsent) return Opt.nil()
        //rotate to perpendicular side's bottom left, we also offset the block by 1 so that it becomes the bottom most outer block
        //we must offset the min by the forward.opposite so that we can get the first outer block
        val perpendicularOpt = locateMinimum(min().offset(forward).offset(UP), forward.clockWise)
        //return when invalid
        if (!perpendicularOpt.isPresent) return Opt.nil()
        val perpendicular = perpendicularOpt().offset(forward.clockWise).offset(UP)
        //At this point, we know that the perpendicular should be the bottom right of the opposite face relative to the passed forward direction.
        //So we can compute the maximum from that block position
        val max = locateMaximum(perpendicular, forward.opposite)
        //return when invalid
        if (!max.isPresent) return Opt.nil()
        //Finally, if all went well, return out optional with the values
        return Opt.of(min() to max())
    }

    /**
     * Computes the bottom left of the given direction starting at the given position
     */
    private fun locateMinimum(pos: BlockPos, forward: Direction): Opt<BlockPos> {
        var block = pos //The current left block
        while (isBlockValidVariant(block, Side, Opt.nil())) {
            block = block.offset(forward.counterClockWise)
        }

        //At this point, we know we've run out of outer's, so it's either air or an edge
        if (!isBlockValidVariant(block, VerticalEdge, Opt.nil())) {
            block = block.offset(forward.clockWise)
            if (!isBlockValidVariant(block, VerticalEdge, Opt.nil())) {
                error { "We iterated to an invalid block! expected a vertical edge but found ${world().getBlockState(block).block.name.string}, at [${block.toShortString()}]" }
                return Opt.nil()
            }
        }
        //Iterate down until we hit something that isn't valid for vertical edges
        while (isBlockValidVariant(block, VerticalEdge, Opt.nil())) {
            block = block.offset(DOWN)
        }
        //We've gone one past so offset back up
        block = block.offset(UP)
        if (!isBlockValidVariant(block, VerticalEdge, Opt.nil())) {
            error { "We iterated to an invalid block! expected a vertical edge but found ${world().getBlockState(block).block.name.string}, at [${block.toShortString()}]" }
            return Opt.nil()
        }
        return Opt.of(block)
    }

    /**
     * Locates the maximum for the given block position/direction
     */
    private fun locateMaximum(pos: BlockPos, forward: Direction): Opt<BlockPos> {
        var block = pos //The current left block
        while (isBlockValidVariant(block, Side, Opt.nil())) {
            block = block.offset(forward.counterClockWise)
        }
        //At this point, we know we've run out of outer's, so it's either air or an edge
        if (!isBlockValidVariant(block, VerticalEdge, Opt.nil())) {
            block = block.offset(forward.clockWise)
            if (!isBlockValidVariant(block, VerticalEdge, Opt.nil())) {
                error { "We iterated to an invalid block! expected a vertical edge but found ${world().getBlockState(block).block.name.string}, at [${block.toShortString()}]" }
                return Opt.nil()
            }
        }
        //Iterate down until we hit something that isn't valid for vertical edges
        while (isBlockValidVariant(block, VerticalEdge, Opt.nil())) {
            block = block.offset(UP)
        }
        //We've gone one past so offset back up
        block = block.offset(DOWN)
        if (!isBlockValidVariant(block, VerticalEdge, Opt.nil())) {
            error { "We iterated to an invalid block! expected a vertical edge but found ${world().getBlockState(block).block.name.string}, at [${block.toShortString()}]" }
            return Opt.nil()
        }
        return Opt.of(block)
    }

    /**
     * This will return true if we can form the multi block
     */
    fun isValid(structureIn: Opt<StructureStore>): Boolean {
        if (world.isAbsent || structureIn.isAbsent) return false
        val level = world()
        val structure = structureIn()
        for ((block, _) in structure[Side]) if (!isValidSideVariant(block, level, structureIn)) return false
        for ((block, _) in structure[Inner]) if (!isValidInnerVariant(block, level, structureIn)) return false
        for ((block, _) in structure[VerticalEdge]) if (!isValidVerticalEdgeVariant(block, level, structureIn)) return false
        for ((block, _) in structure[HorizontalEdge]) if (!isValidHorizontalEdgeVariant(block, level, structureIn)) return false
//        for ((block, _) in structure[Corner]) if (!isValidCornerVariant(block, level, structureIn)) return false
        return true
    }

    /**
     * return true when the side variant is valid, can add custom specifications here relative to side validation
     */
    fun isValidSideVariant(blockPos: BlockPos, level: Level, structure: Opt<StructureStore>): Boolean = isBlockValidVariant(blockPos, Side, structure)

    /**
     * return true when the inner variant is valid, can add custom specifications here relative to inner validation
     */
    fun isValidInnerVariant(blockPos: BlockPos, level: Level, structure: Opt<StructureStore>): Boolean = isBlockValidVariant(blockPos, Inner, structure)

    /**
     * return true when the vertical edge variant is valid, can add custom specifications here relative to vertical edge validation
     */
    fun isValidVerticalEdgeVariant(blockPos: BlockPos, level: Level, structure: Opt<StructureStore>): Boolean =
        isBlockValidVariant(blockPos, VerticalEdge, structure)

    /**
     * return true when the horizontal edge variant is valid, can add custom specifications here relative to horizontal edge validation
     */
    fun isValidHorizontalEdgeVariant(blockPos: BlockPos, level: Level, structure: Opt<StructureStore>): Boolean =
        isBlockValidVariant(blockPos, HorizontalEdge, structure)


    /**
     * Gets the block state from the world instance and calls the isValid method
     */
    fun isBlockValidVariant(blockPos: BlockPos, type: StructureBlockVariant, structure: Opt<StructureStore>): Boolean {
        if (world.isAbsent) return false
        return isValidVariant(blockPos, world().getBlockState(blockPos), type, structure)
    }

    /**
     * Required to check to see if a block at the given position and type is valid
     */
    fun isValidVariant(blockPos: BlockPos, blockState: BlockState, type: StructureBlockVariant, structure: Opt<StructureStore>): Boolean

    /**
     * Attempts to form and validate the multiblock structure, if not valid it will return empty, otherwise it will
     * return an optional contain the structure. It will also update the states of the children based upon their provided
     * [setFormedAt] state.
     */
    fun form(direction: Direction): Opt<StructureStore> {
        val structure = computeStructure(direction)
        if (structure.isAbsent || world.isAbsent) return Opt.nil()
        if (!isValid(Opt.of(structure().store))) return Opt.nil()
        //Iterate each block and update it's state
        val pruned = pruneStructure(structure())
        for ((variant, map) in pruned.store)
            for ((block, dir) in map)
                world().setBlockAndUpdate(block, setFormedAt(block, dir, variant, pruned))
        return Opt.of(pruned)
    }

    /**
     * This will take the given structure and unform it by resetting the states of the blocks contained within it.
     */
    fun unform(structureIn: Opt<StructureStore>) = structureIn.ifPresent {
        if (world.isAbsent) return
        for ((variant, map) in it.store) {
            for ((block, dir) in map) world().setBlockAndUpdate(block, setUnformedAt(block, dir, variant, it))
        }
    }

    /**
     * This will remove all air from the structure. Air can be a valid inner block optionally,
     * but when it is that's a lot of extra block positions to iterate over, so we prune out
     * all the air slots as they aren't relevant to us
     */
    private fun pruneStructure(structure: MultiBlockStructure): StructureStore {
        val storage = StructureStore(structure.min, structure.max)
        world.ifPresent {
            for ((variant, blocks) in structure.store.store) {
                for ((block, dir) in blocks) {
                    val state = it.getBlockState(block)
                    if (!state.isAir) storage.add(variant, block, dir)
                    else debug { "Pruned air block at ${block.str}" }
                }
            }
        }
        return storage
    }

    /**
     * Gets all the slaves present, and casts them to the given type
     */
    @Suppress("UNCHECKED_CAST")
    fun getSlaves(store: Opt<StructureStore>): Set<ISlave<T, *>> {
        if (store.isAbsent || world.isAbsent) return emptySet()
        val set = HashSet<ISlave<T, *>>()
        for ((block, _) in store()) {
            val be = world().getBlockEntity(block) ?: continue
            if (be is ISlave<*, *>) {
                set.add(be as ISlave<T, *>)
            }
        }
        return set
    }


    /**
     * Used to update the state of a given block. By default, it just returns the current state,
     * use this in the override version to just get the default state and update it
     */
    @Suppress("UNCHECKED_CAST")
    fun setFormedAt(blockPos: BlockPos, direction: Direction, variant: StructureBlockVariant, store: StructureStore): BlockState {
        if (world.isAbsent) return Blocks.AIR.defaultBlockState()
        val tile = world().getBlockEntity(blockPos)
        if (tile is ISlave<*, *>) {
            val slave = (tile as ISlave<T, *>)
            slave.master = (Opt.of(this))
            slave.store = Opt.of(store)
        }
        return world().getBlockState(blockPos)
    }

    /**
     * Should return the unformed state for the given block. It will also remove the master connection from the slaves
     */
    @Suppress("UNCHECKED_CAST")
    fun setUnformedAt(blockPos: BlockPos, direction: Direction, variant: StructureBlockVariant, store: StructureStore): BlockState {
        if (world.isAbsent) return Blocks.AIR.defaultBlockState()
        val tile = world().getBlockEntity(blockPos)
        if (tile is ISlave<*, *>) {
            val slave = (tile as ISlave<T, *>)
            slave.master = Opt.nil()
            slave.store = Opt.nil()
        }
        return world().getBlockState(blockPos).block.defaultBlockState()
    }

    /**
     * Updates the slaves to have a reference to their master
     */
    @Suppress("UNCHECKED_CAST")
    fun updateSlaves(structure: Opt<StructureStore>) {
        if (world.isAbsent || structure.isAbsent) return
        for ((pos, _) in structure()) {
            val be = world().getBlockEntity(pos) ?: continue
            if (be is ISlave<*, *>) {
                (be as ISlave<T, *>).master = Opt.of(this)
                be.store = structure
            }
        }
    }

}