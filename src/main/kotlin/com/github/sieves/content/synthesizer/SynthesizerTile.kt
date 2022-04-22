package com.github.sieves.content.synthesizer

import com.github.sieves.Sieves
import com.github.sieves.content.api.ApiTile
import com.github.sieves.content.api.caps.TrackedEnergy
import com.github.sieves.content.api.caps.TrackedInventory
import com.github.sieves.content.link.Links
import com.github.sieves.content.tile.internal.Configuration
import com.github.sieves.registry.Registry
import com.github.sieves.util.getInflatedAAABB
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.network.NetworkHooks
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class SynthesizerTile(pos: BlockPos, state: BlockState) :
    ApiTile<SynthesizerTile>(Registry.Tiles.Synthesizer, pos, state, "tile.sieves.synthesizer"), Nameable {
    private val energyHandler = LazyOptional.of { energy }
    private val itemHandler = LazyOptional.of { items }
    private var tick = 0
    private val removals = ArrayList<BlockPos>()
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(3, ::update)
    val links = Links()
    val powerCost: Int get() = ((targetEnergy) / configuration.efficiencyModifier).roundToInt()
    val sleepTime: Int get() = ((20 * 60) / configuration.speedModifier).roundToInt()
    override val ioPower: Int get() = ((links.getLinks().size * 600) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)
    private var targetResult = ItemStack.EMPTY
    private var targetDamage = 1
    private var isCrafting = false
    var targetEnergy = 0
        private set
    var targetProgress = 1
        private set
    var progress = 0f
        private set
    private var percent = 0

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()
        if (!isCrafting)
            craft()
        else if (extractPower()) {
            if (progress.toInt() % 10 == 1) {
                update()
            }
            progress += configuration.speedModifier
            if (items.getStackInSlot(0).isEmpty || items.getStackInSlot(1).isEmpty || (!items.getStackInSlot(2).isEmpty && !items.getStackInSlot(
                    2
                ).sameItem(
                    targetResult
                ) || items.getStackInSlot(2).count >= items.getStackInSlot(2).maxStackSize)
            ) {
                isCrafting = false
                progress = 0f
                percent = 0
                targetProgress = 1
                targetDamage = 1
                targetResult = ItemStack.EMPTY
                update()
            }
        }

        if (progress >= targetProgress && isCrafting) {
            //
            //Finish craft here
            val items = this.items.extractItem(0, 64, false)
            items.shrink(1)
            this.items.insertItem(0, items, false)
            val tool = this.items.getStackInSlot(1)
            tool.damageValue = tool.damageValue + targetDamage
            if (tool.damageValue >= tool.maxDamage) {
                this.items.extractItem(1, 64, false)
            }
            this.items.insertItem(2, targetResult.copy(), false)
            isCrafting = false
            progress = 0f
            update()
            percent = 0
            targetProgress = 1
            targetDamage = 1
            targetResult = ItemStack.EMPTY
        }

        tick++
    }

    private fun extractPower(): Boolean {
        val extracted = energy.extractEnergy(powerCost, true)
        if (extracted != powerCost) return false
        energy.extractEnergy(powerCost, false)
        return true
    }

    private fun craft() {
//        getRecipeFor(Registry.RecipeTypes.Sieve, container, level!!)
        val recipes = level?.recipeManager?.getAllRecipesFor(Registry.RecipeTypes.Sieve) ?: return
        val input = items.getStackInSlot(0)
        val tool = items.getStackInSlot(1)

        for (recipe in recipes) {
            if (recipe.ingredients[0].test(input) && recipe.ingredients[1].test(tool)) {

                //Valid craft!
//                tool.damageValue = tool.damageValue - it.durability
                targetResult = recipe.result
                targetDamage = recipe.durability
                targetProgress = recipe.time
                targetEnergy = recipe.power
                isCrafting = true
                break
            }
        }
    }

    /**
     * Called when saving nbt data
     */
    override fun onSave(tag: CompoundTag) {
        tag.put("energy", energy.serializeNBT())
        tag.put("links", links.serializeNBT())
        tag.put("items", items.serializeNBT())
        tag.put("config", getConfig().serializeNBT())
        tag.putInt("target", targetProgress)
        tag.putInt("targetEnergy", targetEnergy)
        tag.putFloat("progress", progress)
        tag.putInt("damage", targetDamage)
        tag.put("result", targetResult.serializeNBT())

    }

    /**
     * Called when loading the nbt data
     */
    override fun onLoad(tag: CompoundTag) {
        energy.deserializeNBT(tag.get("energy"))
        links.deserializeNBT(tag.getCompound("links"))
        items.deserializeNBT(tag.getCompound("items"))
        targetResult.deserializeNBT(tag.getCompound("result"))
        targetEnergy = tag.getInt("targetEnergy")
        targetDamage = tag.getInt("damage")
        targetProgress = tag.getInt("target")
        progress = tag.getFloat("progress")
        getConfig().deserializeNBT(tag.getCompound("config"))
    }

    /**
     * This gets the direction based upon the relative side
     */
    override fun getRelative(side: Configuration.Side): Direction = when (side) {
        Configuration.Side.Top -> Direction.UP
        Configuration.Side.Bottom -> Direction.DOWN
        Configuration.Side.Front -> Direction.NORTH
        Configuration.Side.Back -> Direction.SOUTH
        Configuration.Side.Right -> Direction.EAST
        Configuration.Side.Left -> Direction.WEST
    }


    /**
     * This is used to open up the container menu
     */
    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> SynthesizerContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

    /**
     * Called on capability invalidation
     */
    override fun onInvalidate() {
        energyHandler.invalidate()
        itemHandler.invalidate()
    }

    override fun getName(): Component {
        return TranslatableComponent("container.${Sieves.ModId}.synthesizer")
    }

    override fun getRenderBoundingBox(): AABB {
        return INFINITE_EXTENT_AABB
    }


    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (side == null && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return itemHandler.cast()
        if (cap == CapabilityEnergy.ENERGY && side == null) return energyHandler.cast()
        if (side == null) return super.getCapability(cap, null)
        return when (getConfig()[side]) {
            Configuration.SideConfig.InputItem -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputPower -> if (CapabilityEnergy.ENERGY == cap) return energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.OutputItem -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.OutputPower -> if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.None -> LazyOptional.empty()
            Configuration.SideConfig.InputOutputItems -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputOutputPower -> if (CapabilityEnergy.ENERGY == cap) return energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputOutputAll -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
        }

    }
}