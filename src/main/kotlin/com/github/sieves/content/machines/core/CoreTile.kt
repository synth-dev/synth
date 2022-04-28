package com.github.sieves.content.machines.core

import com.github.sieves.api.ApiTile
import com.github.sieves.content.io.link.Links
import com.github.sieves.api.ApiConfig
import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.ApiConfig.SideConfig.*
import com.github.sieves.api.caps.*
import com.github.sieves.recipes.*
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Items
import com.github.sieves.registry.Registry.Net
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.item.*
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*
import net.minecraftforge.energy.*
import net.minecraftforge.fluids.capability.*
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.*
import net.minecraftforge.items.*
import net.minecraftforge.network.NetworkHooks
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class CoreTile(pos: BlockPos, state: BlockState) : ApiTile<CoreTile>(Registry.Tiles.Core, pos, state, "container.synth.core"), Nameable {
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(1, ::update)
    override val tank = TrackedTank(24000, ::update)
    val tankTwo = TrackedTank(24000, ::update)
    private val tankTwoHandler = LazyOptional.of { tankTwo }
    val powerCost: Int get() = (1200 / configuration.efficiencyModifier).roundToInt()
    override val ioPower: Int get() = ((1200) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = (1000 * configuration.speedModifier).roundToInt()
    private var currentRecipe: SolidifierRecipe? = null
    private var tick = 0
    var time = 0
        private set
    var totalTime = 1
        private set

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()
        if (tick >= 20) {
            tryCraft()
            tick = 0
            update()
        }
        updateCraft()
        tick++
    }

    /**
     * Updates the current craft
     */
    private fun updateCraft() {
        if (currentRecipe != null) {
            if (time >= currentRecipe!!.time) {
                finishCraft()
            } else time++
        }
    }

    /**
     * Called upon finishing a craft
     */
    private fun finishCraft() {
        //TODO send packet to client telling them it's finished
        if (currentRecipe == null) return
        val first = currentRecipe!!.fluidIngredients[0]
        val second = currentRecipe!!.fluidIngredients[1]
        if (tank.fluid.fluid.isSame(first.fluidStack.fluid) && tankTwo.fluid.fluid.isSame(second.fluidStack.fluid)) {
            if (tank.fluidAmount >= first.amount && tankTwo.fluidAmount >= second.amount && items.insertItem(
                    0, currentRecipe!!.result, true
                ) == ItemStack.EMPTY
            ) {
                tank.drain(first.amount, EXECUTE)
                tankTwo.drain(second.amount, EXECUTE)
                time = 0
                totalTime = 1
                val result = ItemStack(currentRecipe!!.result.item, 1)
                Net.sendToClientsWithTileLoaded(Net.SolidiferStop {
                    blockPos = this@CoreTile.blockPos
                    item = result
                }, this)
                items.insertItem(0, result, false)
                currentRecipe = null
            }
        } else if (tankTwo.fluid.fluid.isSame(first.fluidStack.fluid) && tank.fluid.fluid.isSame(second.fluidStack.fluid)) if (tankTwo.fluidAmount >= first.amount && tank.fluidAmount >= second.amount && items.insertItem(
                0, currentRecipe!!.result, true
            ) == ItemStack.EMPTY
        ) {
            tankTwo.drain(first.amount, EXECUTE)
            tank.drain(second.amount, EXECUTE)
            time = 0
            totalTime = 1
            val result = ItemStack(currentRecipe!!.result.item, 1)
            Net.sendToClientsWithTileLoaded(Net.SolidiferStop {
                blockPos = this@CoreTile.blockPos
                item = result
            }, this)
            items.insertItem(0, result, false)
            currentRecipe = null
        }
    }


    /**
     * Attempts to craft the recipe
     */
    private fun tryCraft() {
        if (currentRecipe != null) return
        val recipes = level?.recipeManager?.getAllRecipesFor(Registry.RecipeTypes.Solidifier) ?: return
        for (recipe in recipes) {
            val first = recipe.fluidIngredients[0]
            val second = recipe.fluidIngredients[1]
            if (tank.fluid.fluid.isSame(first.fluidStack.fluid) && tankTwo.fluid.fluid.isSame(second.fluidStack.fluid)) {
                if (tank.fluidAmount >= first.amount && tankTwo.fluidAmount >= second.amount) {
//                    tank.drain(first.amount, EXECUTE)
//                    tankTwo.drain(second.amount, EXECUTE)
                    totalTime = recipe.time
                    Net.sendToClientsWithTileLoaded(Net.SolidiferStart {
                        blockPos = this@CoreTile.blockPos
                    }, this)
                    currentRecipe = recipe
                }
            } else if (tankTwo.fluid.fluid.isSame(first.fluidStack.fluid) && tank.fluid.fluid.isSame(second.fluidStack.fluid)) if (tankTwo.fluidAmount >= first.amount && tank.fluidAmount >= second.amount) {
//                tankTwo.drain(first.amount, EXECUTE)
//                tank.drain(second.amount, EXECUTE)
                totalTime = recipe.time
                Net.sendToClientsWithTileLoaded(Net.SolidiferStart {
                    blockPos = this@CoreTile.blockPos
                }, this)
                currentRecipe = recipe
            }
        }
    }

    override fun onInvalidate() {
        super.onInvalidate()
        tankTwoHandler.invalidate()
    }

    /**
     * Block a given side for the tile config
     */
    override fun isSideValidFor(side: SideConfig, direction: Direction): Boolean {
        if (direction != UP && direction != DOWN) //Only allow power from everything but down and up
            return side == SideConfig.InputPower || side == SideConfig.OutputItem
        return side == SideConfig.InputFluid
    }

    //Allow our fluids to be imported from the bottom
    override fun importFluids(value: SideConfig, tile: BlockEntity, key: Direction) {
        if (key == UP) {
            if (value.canImportFluid && tank.capacity > 0) {
                val cap = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    val extracted = other.drain(this.ioRate, SIMULATE)
                    if (tank.fill(extracted, SIMULATE) == extracted.amount) {
                        tank.fill(other.drain(this.ioRate, EXECUTE), EXECUTE)
                    }
                }
            }
        }
        if (key == DOWN) if (value.canImportFluid && tankTwo.capacity > 0) {
            val cap = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, key.opposite)
            if (cap.isPresent) {
                val other = cap.resolve().get()
                val extracted = other.drain(this.ioRate, SIMULATE)
                if (tankTwo.fill(extracted, SIMULATE) == extracted.amount) {
                    tankTwo.fill(other.drain(this.ioRate, EXECUTE), EXECUTE)
                }
            }
        }
    }

    /**
     * Called when saving nbt data
     */
    override fun onSave(tag: CompoundTag) {
        tankTwo.writeToNBT(tag)
        tag.putInt("time", time)
        tag.putInt("total", totalTime)
    }

    /**
     * Called when loading the nbt data
     */
    override fun onLoad(tag: CompoundTag) {
        tankTwo.readFromNBT(tag)
        time = tag.getInt("time")
        totalTime = tag.getInt("total")
    }


    /**
     * This gets the direction based upon the relative side
     */
    override fun getRelative(side: Side): Direction {

        return when (side) {
            Side.Top -> UP
            Side.Bottom -> DOWN
            Side.Front -> NORTH
            Side.Back -> SOUTH
            Side.Right -> EAST
            Side.Left -> WEST
        }
    }

    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (side == null && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && items.slots > 0) return itemHandler.cast()
        if (side == null && cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && tank.capacity > 0) return tankHandler.cast()
        if (side == null && cap == CapabilityEnergy.ENERGY && energy.maxEnergyStored > 0) return energyHandler.cast()
        if (side == null) return super.getCapability(cap, null)
        return when (getConfig()[side]) {
            InputItem, OutputItem, InputOutputItems -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) itemHandler.cast() else LazyOptional.empty()
            InputPower, OutputPower, InputOutputPower -> if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
            InputFluid, OutputFluid, InputOutputFluid -> if (CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY == cap) {
                when (side) {
                    DOWN -> tankTwoHandler.cast()
                    UP -> tankHandler.cast()
                    else -> LazyOptional.empty()
                }
            } else LazyOptional.empty()
            InputOutputAll -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) itemHandler.cast()
            else if (CapabilityEnergy.ENERGY == cap) energyHandler.cast()
            else if (CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY == cap) tankHandler.cast()
            else LazyOptional.empty()
            None -> LazyOptional.empty()
        }
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