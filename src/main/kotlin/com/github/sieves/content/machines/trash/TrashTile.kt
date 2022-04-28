package com.github.sieves.content.machines.trash

import com.github.sieves.Sieves
import com.github.sieves.api.ApiTile
import com.github.sieves.content.io.link.Links
import com.github.sieves.api.ApiConfig
import com.github.sieves.api.caps.*
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Blocks
import com.github.sieves.registry.Registry.Items
import com.github.sieves.registry.Registry.Net
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.item.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.fluids.capability.templates.*
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.network.NetworkHooks
import kotlin.collections.ArrayList
import kotlin.math.*

class TrashTile(pos: BlockPos, state: BlockState) :
    ApiTile<TrashTile>(Registry.Tiles.Trash, pos, state, "tile.synth.trash"), Nameable {
    private var tick = 0
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(21, ::update)
    override val tank: FluidTank = TrackedTank(0, ::update)
    override val ioPower: Int get() = ((links.getLinks().size * 1200) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)
    val links = Links()
    val powerCost: Int get() = (1200 / configuration.efficiencyModifier).roundToInt()

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
//        if (getConfig().autoExport) autoExport()
//        if (getConfig().autoImport) autoImport()
//        if (tick % 20 == 1) link()
//        if (tick >= (20 * 15)) {
//            val height = getHeight()
//            for (slot in 0 until items.slots) {
//                val item = items.getStackInSlot(slot)
//                if (!item.isEmpty && extractPower() && height >= 1) {
//                    items.extractItem(slot, 1, false)
//                    if (level?.random?.nextInt(100)!! <= getHeight()) {
//                        Net.sendToClientsWithTileLoaded(Net.DeleteItem {
//                            this.blockPos = this@TrashTile.blockPos
//                            this.slot = slot
//                            this.item = item
//                            this.result = ItemStack(Items.FluxIron)
//                        }, this)
//                    } else
//                        Net.sendToClientsWithTileLoaded(Net.DeleteItem {
//                            this.blockPos = this@TrashTile.blockPos
//                            this.slot = slot
//                            this.item = item
//                            this.result = ItemStack.EMPTY
//                        }, this)
//                    tick = 0
//                    break
//                }
//            }
//        }
//        tick++
//        links.poll()
    }

    /**
     * Gets the height of multiblock
     */
    fun getHeight(): Int {
        val top = links.getTop()
        if (top == BlockPos.ZERO) return 0
        return min(max(top.y - this.blockPos.y, 0), 100)
    }

    /**
     * Attempts to link any
     */
    private fun link() {
        for (i in 1 until 100) { //100 blocks possible, each one is one percent chance
            val pos = BlockPos(blockPos.x, blockPos.y + i, blockPos.z)
            val state = level?.getBlockState(pos) ?: continue
            if (state.`is`(Blocks.Core)) {
                links.addLink(pos, UP)
            } else break
        }
        for (link in links.getLinks()) {
            val pos = link.key
            val state = level?.getBlockState(pos) ?: continue
            if (!state.`is`(Blocks.Core)) {
                var y = pos.y
                while (y > blockPos.y) {
                    links.queueRemove(BlockPos(this.blockPos.x, y, this.blockPos.z))
                    y--
                }
            }
        }
    }


    private fun extractPower(): Boolean {
        val extracted = energy.extractEnergy(powerCost, true)
        if (extracted != powerCost) return false
        energy.extractEnergy(powerCost, false)
        return true
    }


    /**
     * Called when saving nbt data
     */
    override fun onSave(tag: CompoundTag) {
        tag.put("links", links.serializeNBT())
    }

    /**
     * Called when loading the nbt data
     */
    override fun onLoad(tag: CompoundTag) {
        links.deserializeNBT(tag.getCompound("links"))
    }

    /**
     * This gets the direction based upon the relative side
     */
    override fun getRelative(side: ApiConfig.Side): Direction = when (side) {
        ApiConfig.Side.Top -> Direction.UP
        ApiConfig.Side.Bottom -> Direction.DOWN
        ApiConfig.Side.Front -> NORTH
        ApiConfig.Side.Back -> Direction.SOUTH
        ApiConfig.Side.Right -> Direction.EAST
        ApiConfig.Side.Left -> Direction.WEST
    }


    /**
     * This is used to open up the container menu
     */
    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> TrashContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

}