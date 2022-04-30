package com.github.sieves.content.reactor.control

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.io.link.*
import com.github.sieves.content.reactor.spark.*
import com.github.sieves.registry.Registry.Blocks
import com.github.sieves.registry.Registry.Tiles
import com.github.sieves.util.Log.info
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.InteractionResult.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.*
import net.minecraft.world.phys.*

/**
 * The "brain" of the reactor. Is responsible for updating the states of the casings, finding input/outputs/and sparks
 */
class ControlTile(blockPos: BlockPos, blockState: BlockState) : ReactorTile<ControlTile>(Tiles.Control, blockPos, blockState) {
    private var tick = 0
    val multiBlock = MultiBlock(blockPos)

    //Only stores the sparks that are adjacent to input blocks
    internal val sparks = Links()

    /**
     * Called upon destroying the block
     */
    override fun onDestroy(player: Player, willHarvest: Boolean, fluid: FluidState) {
        level?.let { multiBlock.unform(it) }
    }

    override fun onUseServer(player: ServerPlayer, itemUsed: ItemStack, direction: Direction): InteractionResult {
        level?.let { multiBlock.form(it, direction.opposite) }
        //Attempt to link and form multiblock
        return CONSUME
    }


    /**
     * Delegate ticking of the server here
     */
    override fun onServerTick() {
        if (tick >= 10) {
            if (level != null) {
                if (!multiBlock.checkBlocks(level!!))
                    update()
            }
            if (multiBlock.isFormed) {
                multiBlock.updateBounds()
                for (x in multiBlock.min.x..multiBlock.max.x)
                    for (y in multiBlock.min.y..multiBlock.max.y)
                        for (z in multiBlock.min.z..multiBlock.max.z) {
                            processChild(BlockPos(x, y, z))
                        }
            }
            tick = 0
        }
        tick++
    }

    /**
     * Process the inner block
     */
    private fun processChild(blockPos: BlockPos) {
        if (this.sparks.contains(blockPos)) {
            if (level?.getBlockEntity(blockPos) !is SparkTile) {
                sparks.removeLink(blockPos)
                info { "Removed spark at: ${blockPos.toShortString()}" }
                update()
                return
            }
        }
        val be = level?.getBlockEntity(blockPos)
        if (be !is SparkTile) {
            if (sparks.contains(blockPos)) {
                sparks.removeLink(blockPos)
                info { "Removed spark at: ${blockPos.toShortString()}" }
                update()
            }
            return
        }
        if (be.ctrl.isAbsent || be.ctrlPos == BlockPos.ZERO)
            be.ctrlPos = (this.blockPos)
        val behind = be.neighborState(Side.Front, false)
        if (behind.`is`(Blocks.Input) && !sparks.contains(blockPos)) {
            sparks.addLink(blockPos)
            info { "Added spark root at: ${blockPos.toShortString()}" }
            update()
        }
        if (this.sparks.contains(blockPos) && !behind.`is`(Blocks.Input)) {
            sparks.removeLink(blockPos)
            info { "Removed spark at: ${blockPos.toShortString()}" }
            update()
        }
    }

    override fun onSave(tag: CompoundTag) {
        tag.put("multiblock", multiBlock.serializeNBT())
        tag.put("sparks", sparks.serializeNBT())
    }

    override fun onLoad(tag: CompoundTag) {
        multiBlock.deserializeNBT(tag.getCompound("multiblock"))
        sparks.deserializeNBT(tag.getCompound("sparks"))
    }
}