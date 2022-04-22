package com.github.sieves.content.link

import com.github.sieves.util.getBlockPos
import com.github.sieves.util.putBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.common.util.INBTSerializable

/**
 * Used to link together one to many blocks
 */
class Links() : INBTSerializable<CompoundTag> {
    private val links = HashMap<BlockPos, ArrayList<Direction>>()

    fun getLinks(): Map<BlockPos, List<Direction>> {
        return links
    }

    fun addLink(other: BlockPos, direction: Direction) {
        val list = links.getOrPut(other) { ArrayList() }
        if (!list.contains(direction))
            list.add(direction)
    }

    fun removeLink(pos: BlockPos, direction: Direction? = null) {
        if (direction == null) links.remove(pos)
        else links[pos]?.remove(direction)
    }

    fun removeLinks() {
        links.clear()
    }


    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        val size = links.size
        tag.putInt("count", size)
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