package com.github.sieves.content.io.link

import com.github.sieves.dsl.*
import com.google.common.collect.*
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.nbt.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraftforge.common.util.*
import java.util.*
import kotlin.reflect.*

/**
 * Used to link together one to many blocks
 */
class Links() : INBTSerializable<CompoundTag>, Iterable<BlockPos> {
    private val links = HashMap<BlockPos, ArrayList<Direction>>()
    private val toRemove: Queue<BlockPos> = Queues.newArrayDeque()
    private var top: BlockPos = BlockPos(0, -64, 0)

    val size: Int get() = links.size

    fun contains(blockPos: BlockPos): Boolean = links.containsKey(blockPos)

    fun getLinks(): Map<BlockPos, List<Direction>> {
        return links
    }

    fun getTop(): BlockPos {
        return top
    }

    fun add(other: BlockPos, direction: Direction) {
        if (other.y > top.y) top = other
        val list = links.getOrPut(other) { ArrayList() }
        if (!list.contains(direction)) list.add(direction)
    }


    /**
     * allows for iteration over the inlined block entity types retrieved from the level
     */
    inline fun <reified T : BlockEntity> forEach(level: Level, consumer: (T) -> Unit) {
        for (link in getLinks().keys) {
            val be = level.getBlockEntity(link)
            if (T::class.isInstance(be))
                consumer(T::class.cast(be))
        }
    }

    /**
     * Returns a collection containing only the filtered out block entities
     */
    inline fun <reified T : BlockEntity> instancesOf(level: Level): Iterable<T> {
        return getLinks().keys.mapNotNull { level.getBlockEntity(it) }.filterIsInstance<T>()
    }

    fun add(other: BlockPos) = add(other, NORTH)

    fun remove(pos: BlockPos, direction: Direction? = null) {
        if (direction == null) links.remove(pos)
        else links[pos]?.remove(direction)
        if (pos == top) {
            var highest = BlockPos.ZERO
            for (block in links.keys) {
                if (block.y > highest.y) highest = block
            }
            top = highest
        }
    }

    fun queueRemove(pos: BlockPos) {
        toRemove.add(pos)
    }

    fun poll() {
        while (!toRemove.isEmpty()) {
            remove(toRemove.poll())
        }
    }

    fun removeLinks() {
        links.clear()
    }


    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        val size = links.size
        tag.putInt("count", size)
        tag.putBlockPos("top", this.top)
        var i = 0
        links.forEach { (t, u) ->
            tag.putBlockPos("pos_${i}", t)
            val values = CompoundTag()
            values.putInt("size", u.size)
            u.forEachIndexed { index, direction ->
                values.putInt("dir_${index}", direction.ordinal)
            }
            tag.put("link_${i++}", values)
        }
        return tag
    }

    override fun deserializeNBT(nbt: CompoundTag) {
        val size = nbt.getInt("count")
        links.clear()
        this.top = nbt.getBlockPos("top").bp
        for (i in 0 until size) {
            val pos = nbt.getBlockPos("pos_${i}")
            val values = nbt.getCompound("link_${i}")
            val count = values.getInt("size")
            val list = links.getOrPut(pos.bp) { ArrayList() }
            for (j in 0 until count) {
                val dir = Direction.values()[values.getInt("dir_${j}")]
                list.add(dir)
            }
        }
    }

    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<BlockPos> = links.keys.iterator()
}