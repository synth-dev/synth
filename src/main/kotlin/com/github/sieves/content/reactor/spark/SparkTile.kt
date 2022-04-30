package com.github.sieves.content.reactor.spark

import com.github.sieves.api.caps.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.io.link.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.registry.*
import com.github.sieves.registry.Registry.Items
import com.github.sieves.util.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.*
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.util.LazyOptional
import java.util.UUID

/**
 * Keeps track of the internal input buffer of items being pumped into the controller
 */
class SparkTile(blockPos: BlockPos, blockState: BlockState) : ReactorTile<SparkTile>(Registry.Tiles.Spark, blockPos, blockState) {
    private var tick = 0
    private val links = Links()
    private val buffer = TrackedInventory(1, ::update)
    private val bufferHandler = LazyOptional.of { buffer }

    val isRoot: Boolean
        get() = if (ctrl.isAbsent) false else {
            ctrl().sparks.contains(blockPos)
        }

    /**
     * Will only tick when the controller is present
     */
    override fun onServerTick() = ctrl.ifPresent {
        if (tick >= 10) {

        }
        tick++
    }


    /**
     * Do nbt saving here
     */
    override fun onSave(tag: CompoundTag) {
        tag.put("links", links.serializeNBT())
        tag.put("buffer", buffer.serializeNBT())
    }

    /**
     * Load nbt data here
     */
    override fun onLoad(tag: CompoundTag) {
        links.deserializeNBT(tag.getCompound("links"))
        buffer.deserializeNBT(tag.getCompound("buffer"))
    }
}