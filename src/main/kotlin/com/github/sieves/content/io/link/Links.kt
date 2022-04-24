package com.github.sieves.content.io.link

import com.github.sieves.util.getBlockPos
import com.github.sieves.util.putBlockPos
import com.google.common.collect.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.common.util.INBTSerializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Used to link together one to many blocks
 */
class Links() : INBTSerializable<CompoundTag> {
    private val links = HashMap<BlockPos, ArrayList<Direction>>()
    private val toRemove: Queue<BlockPos> = Queues.newArrayDeque()
    private var top: BlockPos = BlockPos(0, -64, 0)

    fun getLinks(): Map<BlockPos, List<Direction>> {
        return links
    }

    fun getTop(): BlockPos {
        return top
    }

    fun addLink(other: BlockPos, direction: Direction) {
        if (other.y > top.y) top = other
        val list = links.getOrPut(other) { ArrayList() }
        if (!list.contains(direction)) list.add(direction)
    }

    fun removeLink(pos: BlockPos, direction: Direction? = null) {
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
            removeLink(toRemove.poll())
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
        this.top = nbt.getBlockPos("top")
        for (i in 0 until size) {
            val pos = nbt.getBlockPos("pos_${i}")
            val values = nbt.getCompound("link_${i}")
            val count = values.getInt("size")
            val list = links.getOrPut(pos) { ArrayList() }
            for (j in 0 until count) {
                val dir = Direction.values()[values.getInt("dir_${j}")]
                list.add(dir)
            }
        }
    }

}