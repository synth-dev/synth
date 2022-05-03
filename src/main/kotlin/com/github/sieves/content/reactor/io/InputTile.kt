package com.github.sieves.content.reactor.io

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.multiblock.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.content.reactor.core.*
import com.github.sieves.dsl.*
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Items
import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.state.*
import net.minecraftforge.energy.*
import net.minecraftforge.fluids.capability.*
import net.minecraftforge.items.*

/**
 * Keeps track of the internal input buffer of items being pumped into the controller
 */
class InputTile(blockPos: BlockPos, blockState: BlockState) : BaseTile<InputTile>(Registry.Tiles.Input, blockPos, blockState),
    ISlave<ControlTile, InputTile> {
    /**Get and set the master instance**/
    override var master: Opt<IMaster<ControlTile>> = Opt.nil()

    /**Used for interactions with the master**/
    override var store: Opt<StructureStore> = Opt.nil()


    /**Keep track of tick to do logic at fixed rate**/
    private var tick = 0

    /**
     * Will only tick when the controller is present
     */
    override fun onTick(level: Level) {
        if (level.isClientSide || !master) return
        tryUpdateState()
        distributeDust()
    }

    /**
     * Extracts exactly 1 item per tick from the facing inventory and directly inserts it into the controller tile only if it's empty,
     * so that means there is basically zero internal fuel buffer.
     */
    private fun distributeDust() {
        if (!store || !world) return
        val pos = this.pos.offset(relativeDir(Side.Front))

        val be = world().getBlockEntity(pos)
        val cap = be?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ?: return
        if (cap.isPresent) {
            val otherInv = cap.resolve().get()
            for (slot in 0 until otherInv.slots) {
                val extracted = otherInv.extractItem(slot, 1, true)
                if (extracted.`is`(Items.FluxDust)) {
                    var escape = false
                    master {
                        for (slave in get<ChamberTile>(store)) {
                            val invCap = slave.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                            if (!invCap.isPresent) continue
                            val inv = invCap.resolve().get()
                            if (extracted.isEmpty) continue
                            val result = inv.insertItem(0, extracted, false)
                            if (result.isEmpty) { //Insert the items into the spark, break upon inserting. only 1 item allowed into any sparks per tick. 1 total allowed in spark
                                otherInv.extractItem(slot, 1, false)
                                escape = true
                                break
                            }
                        }
                    }
                    if (escape) break
                }
            }
        }
    }

    /**
     * Attempts to update the state if possible
     */
    private fun tryUpdateState() {
        val front = getNeighbor(Side.Front)
        val be = level?.getBlockEntity(front)
        val valid = be?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)?.isPresent == true
                || be?.getCapability(CapabilityEnergy.ENERGY)?.isPresent == true
                || be?.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)?.isPresent == true
        level?.setBlockAndUpdate(
            blockPos, blockState.setValue(InputBlock.Piped, valid)
        )
    }


}