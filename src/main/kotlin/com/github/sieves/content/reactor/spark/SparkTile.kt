package com.github.sieves.content.reactor.spark

import com.github.sieves.api.multiblock.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.dsl.*
import com.github.sieves.registry.Registry
import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.state.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*
import net.minecraftforge.items.*

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