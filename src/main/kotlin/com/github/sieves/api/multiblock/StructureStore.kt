package com.github.sieves.api.multiblock

import com.github.sieves.util.*
import com.google.common.collect.Iterators
import net.minecraft.core.*
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.common.util.INBTSerializable
import java.util.*
import kotlin.collections.MutableMap.*

class StructureStore(private val blocks: MutableMap<StructureBlockVariant, MutableMap<BlockPos, Direction>>, minIn: Vec3i, maxIn: Vec3i) :
    INBTSerializable<CompoundTag>, Iterable<MutableEntry<BlockPos, Direction>> {
    /**Keep track of the min block position, allowing for it to be serialized**/
    var min: BlockPos = minIn.bp
        private set
    var max: BlockPos = maxIn.bp
        private set


    /**For ease of access**/
    val variants: Array<StructureBlockVariant> = StructureBlockVariant.values()

    /**Allows for immutable reading of the internal storage buffer**/
    val store: Map<StructureBlockVariant, Map<BlockPos, Direction>> get() = blocks

    /**
     * Allow for zero arg constructor for mutablity
     */
    constructor(min: Vec3i, max: Vec3i) : this(EnumMap(StructureBlockVariant::class.java), min, max)

    /**
     * Allows for structores to be created from tags
     */
    constructor(tag: CompoundTag) : this(tag.getBlockPos("min"), tag.getBlockPos("max")) {
        deserializeNBT(tag)
    }


    /**
     * Will add the given BlockPos to the variant's set
     */
    fun add(variant: StructureBlockVariant, blockPos: BlockPos, facing: Direction): Boolean {
        val map = blocks.getOrPut(variant) { hashMapOf() }
        if (map.containsKey(blockPos)) return false
        map[blockPos] = facing
        return true
    }

    /**
     * Attempts to remove any occurrence of the given blockPos in any variant set.
     * returns true if successfully removed
     */
    fun remove(blockPos: BlockPos): Boolean {
        for ((_, blocks) in blocks) if (blocks.remove(blockPos) != null) return true
        return false
    }

    /**
     * Get the set of variant blocks
     */
    operator fun get(variant: StructureBlockVariant): Map<BlockPos, Direction> = blocks.getOrDefault(variant, hashMapOf())

    /**
     * Saves our blocks to compound tag
     */
    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        val map = hashMapOf<StructureBlockVariant, CompoundTag>()
        for ((variant, values) in blocks) map[variant] = values.toCompound()
        tag.putMap("block_store", map)
        tag.putBlockPos("min", min)
        tag.putBlockPos("max", max)
        return tag
    }

    /**
     * Reads our blocks from a compound tag
     */
    override fun deserializeNBT(tagIn: CompoundTag) {
        this.blocks.clear()
        for ((variant, tag) in tagIn.getMap<StructureBlockVariant, CompoundTag>("block_store")) {
            val values = tag.toMap<BlockPos, Direction>()
            this.blocks[variant] = values
        }
        this.min = tagIn.getBlockPos("min").bp
        this.max = tagIn.getBlockPos("max").bp
    }

    /**
     * Iterates over every single block position contained within this
     */
    override fun iterator(): Iterator<MutableEntry<BlockPos, Direction>> {
        val iterators = blocks.values.map { it.iterator() }.toTypedArray()
        return Iterators.concat(*iterators)
    }
}