package com.github.sieves.content.reactor.spark

import com.github.sieves.api.caps.*
import com.github.sieves.api.multiblock.*
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
import net.minecraft.world.level.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.CapabilityItemHandler
import java.util.UUID

/**
 * Keeps track of the internal input buffer of items being pumped into the controller
 */
class SparkTile(blockPos: BlockPos, blockState: BlockState) : BaseTile<SparkTile>(Registry.Tiles.Spark, blockPos, blockState), ISlave<ControlTile, SparkTile> {
    private var tick = 0
    private val items by handlerOf<Delegates.Items>("items", 1)
    /**
     * Will only tick when the controller is present
     */
    override fun onTick(level: Level) = master.ifPresent {
        if (tick >= 10) {

        }
        tick++
    }

    /**
     * Return out item handler capability
     */
    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return items.cast()
        return super.getCapability(cap, side)
    }

    /**Get and set the master instance**/
    override var master: Opt<IMaster<ControlTile>> = Opt.nil()

    /**Used for interactions with the master**/
    override var store: Opt<StructureStore> = Opt.nil()
}