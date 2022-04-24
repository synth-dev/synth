package com.github.sieves.content.machines.core

import com.github.sieves.api.ApiTile
import com.github.sieves.content.io.link.Links
import com.github.sieves.api.ApiConfig
import com.github.sieves.api.caps.*
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Items
import com.github.sieves.registry.Registry.Net
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.item.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.network.NetworkHooks
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class CoreTile(pos: BlockPos, state: BlockState) :
    ApiTile<CoreTile>(Registry.Tiles.Core, pos, state, "tile.synth.core"), Nameable {
    private var tick = 0
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(1, ::update)
    override val fluids = TrackedTank(0, ::update)
    val links = Links()
    val powerCost: Int get() = (1200 / configuration.efficiencyModifier).roundToInt()
    override val ioPower: Int get() = ((links.getLinks().size * 1200) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()
        if (tick >= (20 * 7)) {
            for (slot in 0 until items.slots) {
                val item = items.getStackInSlot(slot)
                if (!item.isEmpty && extractPower()) {
                    items.extractItem(slot, 1, false)

                    if (level?.random?.nextInt(1000)!! < 50) {
                        Net.sendToClientsWithTileLoaded(Net.DeleteItem {
                            this.blockPos = this@CoreTile.blockPos
                            this.slot = slot
                            this.item = item
                            this.result = ItemStack(Items.FluxIron)
                        }, this)
                    } else
                        Net.sendToClientsWithTileLoaded(Net.DeleteItem {
                            this.blockPos = this@CoreTile.blockPos
                            this.slot = slot
                            this.item = item
                            this.result = ItemStack.EMPTY
                        }, this)
                    tick = 0
                    break
                }
            }
        }
        tick++
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
        ApiConfig.Side.Front -> Direction.NORTH
        ApiConfig.Side.Back -> Direction.SOUTH
        ApiConfig.Side.Right -> Direction.EAST
        ApiConfig.Side.Left -> Direction.WEST
    }

    /**
     * This is used to open up the container menu
     */
    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> CoreContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }


    override fun getRenderBoundingBox(): AABB {
        return INFINITE_EXTENT_AABB
    }


}