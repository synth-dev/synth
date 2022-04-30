package com.github.sieves.content.machines.materializer

import com.github.sieves.api.*
import com.github.sieves.api.caps.*
import com.github.sieves.content.io.link.*
import com.github.sieves.content.machines.materializer.MaterializerCraft.*
import com.github.sieves.registry.Registry.Net
import com.github.sieves.registry.Registry.Tiles
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.energy.*
import net.minecraftforge.fluids.capability.templates.*
import net.minecraftforge.items.*
import net.minecraftforge.network.*
import kotlin.math.*

class MaterializerTile(pos: BlockPos, state: BlockState) :
    ApiTile<MaterializerTile>(Tiles.Materializer, pos, state, "container.synth.materializer") {
    override val energy: EnergyStorage = TrackedEnergy(250000, ::update)
    override val items: ItemStackHandler = TrackedInventory(11, ::update)
    val powerCost: Int get() = (craft.power / configuration.efficiencyModifier).roundToInt()
    override val tank: FluidTank = TrackedTank(0, ::update)
    override val ioPower: Int get() = (1200 * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)
    val links = Links()

    //Store our current crafting item
    var craft: MaterializerCraft = MaterializerCraft.Empty
        private set

    override fun onServerTick() {
        if (configuration.autoExport) autoExport()
        if (configuration.autoImport) autoImport()
        if (craft.isEmpty) attemptCraft()
        else if (craft.currentTime < (craft.time / configuration.speedModifier)) updateCraft()
        else finishCraft()
    }

    /**
     * Attempts to find the correct crafting recipe
     */
    private fun attemptCraft() {
        craft.currentTime = 0
        val craftResult =
            level?.recipeManager?.let { MaterializerCraftManager.findRecipe(it, items.getStackInSlot(0)) } ?: return
        if (!craftResult.isEmpty) {
            val extracted = items.extractItem(0, 1, true)
            if (extracted.sameItem(craftResult.input)) {
                items.extractItem(0, 1, false)
                this.craft = craftResult
                Net.sendToClientsWithTileLoaded(Net.MaterializerStart {
                    blockPos = this@MaterializerTile.blockPos
                    craft = this@MaterializerTile.craft
                }, this)
            }
        }
    }

    /**
     * Update the current crafting time
     */
    private fun updateCraft() {
        //Update the current crafting time only if we extracted the required power
        if (extractPower()) {
            craft.currentTime++
            if (craft.currentTime % 20 == 0) //update client every second, only if we extracted the power
                update()
        }

//        if (!items.getStackInSlot(0).sameItem(craft.input) && !craft.isEmpty) {
//            craft = MaterializerCraft.Empty
//        }
    }

    /**
     * Extract our target power
     */
    private fun extractPower(): Boolean {
        val extracted = energy.extractEnergy(powerCost, true)
        if (extracted != powerCost) return false
        energy.extractEnergy(powerCost, false)
        return true
    }

    /**
     * Called upon completion of the craft
     */
    private fun finishCraft() {
        var inserted = false
        if (craft.output.isEmpty()) {
            craft = MaterializerCraft.Empty
            return
        }
        val output = craft.output.firstOrNull() ?: ItemStack.EMPTY
        if (output == ItemStack.EMPTY) {
            craft = MaterializerCraft.Empty
            return
        }
        output.count = 1
        for (slot in 1 until 11) {
            if (items.insertItem(slot, output, true) == ItemStack.EMPTY) {
//                items.setStackInSlot(slot, output)
                items.insertItem(slot, output, false)
                craft = MaterializerCraft.Empty
                update()
                return
            }
        }
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

    override fun onLoad(tag: CompoundTag) {
        links.deserializeNBT(tag.getCompound("links"))
        craft.deserializeNBT(tag.getCompound("craft"))
    }

    override fun onSave(tag: CompoundTag) {
        tag.put("links", links.serializeNBT())
        tag.put("craft", craft.serializeNBT())
    }


    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> MaterializerContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

}