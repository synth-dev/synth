package com.github.sieves.content.reactor.io

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.caps.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.reactor.spark.*
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Items
import com.github.sieves.util.*
import com.github.sieves.util.Log.info
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemHandlerHelper

/**
 * Keeps track of the internal input buffer of items being pumped into the controller
 */
class InputTile(blockPos: BlockPos, blockState: BlockState) : ReactorTile<InputTile>(Registry.Tiles.Input, blockPos, blockState) {
    private var tick = 0
    private val buffer = TrackedInventory(1, ::update)
    private val bufferHandler = LazyOptional.of { buffer }
    private var lastId = 0

    /**
     * Will only tick when the controller is present
     */
    override fun onServerTick() = ctrl.ifPresent {
        if (tick >= 10) {
            tryUpdateState()
            extract()
            level?.let { it1 ->
                it.sparks.forEach<SparkTile>(it1) {

                }
            }
            tick = 0
        }
        tick++
    }

    /**
     * Extracts the items from the touching inventories to our local inventory
     */
    private fun extract() {
        val forward = blockState.getValue(HorizontalDirectionalBlock.FACING)
        val tile = level?.getBlockEntity(blockPos.offset(forward)) ?: return
        val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, forward.opposite)
        if (cap.isPresent) {
            val inv = cap.resolve().get()
            for (slot in 0 until inv.slots) {
                val stack = inv.getStackInSlot(slot)
                if (stack.`is`(Items.FluxDust)) {
                    val items = inv.extractItem(slot, 64, true)
                    val rem = ItemHandlerHelper.insertItem(this.buffer, items, false)
                    if (rem.isEmpty)
                        info { "Extracted [${items.count - rem.count}] ${stack.displayName.string}" }
                    inv.extractItem(slot, items.count - rem.count, false)
                }
            }
        }
    }

    /**
     * Attempts to update the state if possible
     */
    private fun tryUpdateState() {
        val front = neighborPos(Side.Front)
        val be = level?.getBlockEntity(front)
        level?.setBlockAndUpdate(
            blockPos, blockState.setValue(InputBlock.Piped, be?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)?.isPresent == true)
        )
    }

    override fun onInvalidate() {
        bufferHandler.invalidate()
    }

    override fun onSave(tag: CompoundTag) {
        tag.put("buffer", buffer.serializeNBT())
    }

    override fun onLoad(tag: CompoundTag) {
        buffer.deserializeNBT(tag.getCompound("buffer"))
    }

    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (side == null) return LazyOptional.empty()
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return bufferHandler.cast()
        }
        return super.getCapability(cap, side)
    }


}