package com.github.sieves.api.tab

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Store our tab information in global world data here
 */
class TabData(val tag: CompoundTag? = null) : SavedData() {
    val tabs: MutableMap<UUID, ConcurrentHashMap<ResourceLocation, CompoundTag>> = ConcurrentHashMap()

    init {
        tag?.let {
            for (i in 0 until it.getInt("count")) {
                val new = tag.getCompound("tag_${i}")
                val uuid = new.getUUID("uuid")
                val map = ConcurrentHashMap<ResourceLocation, CompoundTag>()
                for (j in 0 until new.getInt("count")) {
                    val value = new.getCompound("it_$j")
                    val res = ResourceLocation(value.getString("namespace"), value.getString("path"))
                    val tag = value.getCompound("tag")
                    map[res] = tag
                }
                tabs[uuid] = map
            }
        }
    }

    /**
     * Used to save the `SavedData` to a `CompoundTag`
     * @param pCompoundTag the `CompoundTag` to save the `SavedData` to
     */
    override fun save(tag: CompoundTag): CompoundTag {
//        this.tag?.let { tag.put("tab_data", it) }
        tag.putInt("count", tabs.size)
        var i = 0
        for ((key, value) in tabs) {
            val new = CompoundTag()
            new.putUUID("uuid", key)
            val size = value.size
            new.putInt("count", size)
            var j = 0
            for ((res, compound) in value) {
                val it = CompoundTag()
                it.putString("namespace", res.namespace)
                it.putString("path", res.path)
                it.put("tag", compound)
                new.put("it_${j++}", it)
            }

            tag.put("tag_${i++}", new)
        }
        return tag
    }


    companion object {
        operator fun get(level: Level): TabData {
            if (level.isClientSide) error("Only should be called from server!")
            val store = (level as ServerLevel).dataStorage
            return store.computeIfAbsent({
                                             TabData(it)
                                         }, {
                                             TabData()
                                         }, "tab_data")
        }
    }

}