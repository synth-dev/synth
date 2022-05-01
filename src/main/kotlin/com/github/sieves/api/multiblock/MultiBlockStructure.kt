package com.github.sieves.api.multiblock

import com.github.sieves.api.multiblock.StructureBlockVariant.*
import com.github.sieves.util.*
import com.github.sieves.util.Log.debug
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.*
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.AABB
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.MutableMap.*
import kotlin.math.*

/**
 * Stores the boundaries for the multiblock structure
 */
data class MultiBlockStructure(val min: Vec3i, val max: Vec3i) : Iterable<MutableEntry<BlockPos, Direction>> {
    /**The size of the cube, computed via max-min**/
    val size: Vec3i = Vec3i(max.x - min.x, max.y - min.y, max.z - min.z)

    /**An aabb with the world-position transform relative to the inside the multiblock**/
    val inside: AABB = AABB(min.offset(1, 1, 1).f, max.offset(1, 1, 1).f)

    /**Used for iterating over all the block sets via the variants**/
    val variants: Array<StructureBlockVariant> = StructureBlockVariant.values()

    /**Keeps track of the internal structure**/
    val store = StructureStore(mapBlocks(EnumMap(StructureBlockVariant::class.java)), min, max)

    /**
     * Iterates through the blocks adding the correct types
     */
    private fun mapBlocks(map: MutableMap<StructureBlockVariant, MutableMap<BlockPos, Direction>>): MutableMap<StructureBlockVariant, MutableMap<BlockPos, Direction>> {
        val xr = min(min.x, max.x)..max(min.x, max.x)
        val yr = min(min.y, max.y)..max(min.y, max.y)
        val zr = min(min.z, max.z)..max(min.z, max.z)
        for (x in xr) {
            for (z in zr) {
                for (y in yr) {
                    mapBlock(x, y, z, map)
                }
            }
        }
        return map
    }

    /**
     * Checks the block at the given position and adds it to the correct set depending on the multitype
     */
    private fun mapBlock(x: Int, y: Int, z: Int, map: MutableMap<StructureBlockVariant, MutableMap<BlockPos, Direction>>) {
        //If we're here, we know we're we're checking the outside

        if (y == max.y && x > min.x && x < max.x && z > min.z && z < max.z) {
            val set = map.getOrPut(Side) { hashMapOf() }
            set[BlockPos(x, y, z)] = getDirectionFor(x, y, z)
            debug { "Found outer block at [$x, $y, $z]" }
            return
        }
        if (x == min.x || z == min.z || x == max.x || z == max.z) {
            //We know at this point we've found a horizontal edge
            if (y == max.y || y == min.y) {
                val set = map.getOrPut(HorizontalEdge) { hashMapOf() }
                set[BlockPos(x, y, z)] = getDirectionFor(x, y, z)
                debug { "Found horizontal edge block at [$x, $y, $z]" }
            }
            //At this point we know it's not a horizontal edge, so we check to see if it's a corner
            if ((x == min.x && z == min.z) || (x == max.x && z == min.z) || (x == min.x && z == max.z) || (x == max.x && z == max.z)) {
                if (y == min.y || y == max.y) {
                    val set = map.getOrPut(Corner) { hashMapOf() }
                    set[BlockPos(x, y, z)] = getDirectionFor(x, y, z)
                    debug { "Found corner block at [$x, $y, $z]" }
                    return
                } else {
                    val set = map.getOrPut(VerticalEdge) { hashMapOf() }
                    set[BlockPos(x, y, z)] = getDirectionFor(x, y, z)
                    debug { "Found vertical edge block at [$x, $y, $z]" }
                    return
                }
            }
            val set = map.getOrPut(Side) { hashMapOf() }
            set[BlockPos(x, y, z)] = getDirectionFor(x, y, z)
            debug { "Found outer block at [$x, $y, $z]" }
            return
        }
        if (x < max.x && x > min.x && y > min.y && y < max.y && z > min.z && z < max.z) {
            val set = map.getOrPut(Inner) { hashMapOf() }
            set[BlockPos(x, y, z)] = getDirectionFor(x, y, z)
            debug { "Found inner block at [$x, $y, $z]" }
            return
        }
    }

    /**
     * Returns a direction for the given face relative to the min/max
     */
    private fun getDirectionFor(x: Int, y: Int, z: Int): Direction {
        val max = Vec3i(max(this.min.x, this.max.x), max(this.min.y, this.max.y), max(this.min.z, this.max.z))
        val min = Vec3i(min(this.min.x, this.max.x), min(this.min.y, this.max.y), min(this.min.z, this.max.z))

//        if (x == max.x && z == max.z) return if (y == max.y) UP else if (y == min.y) DOWN else WEST
//        if (x == max.x && z == min.z) return if (y == max.y) UP else if (y == min.y) DOWN else SOUTH
//        if (x == min.x && z == max.z) return if (y == max.y) UP else if (y == min.y) DOWN else NORTH
//        if (x == min.x && z == min.z) return if (y == max.y) UP else if (y == min.y) DOWN else EAST
        if (x == max.x && z == max.z) return WEST
        if (x == max.x && z == min.z) return SOUTH
        if (x == min.x && z == max.z) return NORTH
        if (x == min.x && z == min.z) return EAST
        if (z == max.z) return NORTH
        if (z == min.z) return SOUTH
        if (x == min.x) return EAST
        if (x == max.x) return WEST
        if (y == max.y) return UP
        if (y == min.y) return DOWN
        return NORTH
    }

    /**
     * Simply gets the blocks of the given type
     */
    operator fun get(multiType: StructureBlockVariant): Map<BlockPos, Direction> = store[multiType]


    override fun toString(): String {
        val count = store.count()
        return "MultiBounds(min=[${min.toShortString()}], max=[${max.toShortString()}], area=[${size.toShortString()}], vertical=${get(VerticalEdge).size}, horizontal=${
            get(
                HorizontalEdge
            ).size
        }, inner=${get(Inner).size}, outer=${get(Side).size}, total=${count})"
    }

    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<MutableEntry<BlockPos, Direction>> = store.iterator()
}