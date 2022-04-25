package com.github.sieves.content.machines.synthesizer

import com.github.sieves.api.ApiTile
import com.github.sieves.content.io.link.Links
import com.github.sieves.api.ApiConfig
import com.github.sieves.api.caps.*
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Sounds
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource.*
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.fluids.capability.templates.*
import net.minecraftforge.network.NetworkHooks
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class SynthesizerTile(pos: BlockPos, state: BlockState) :
    ApiTile<SynthesizerTile>(Registry.Tiles.Synthesizer, pos, state, "tile.synth.synthesizer"), Nameable {
    private var tick = 0
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(3, ::update)
    override val fluids: FluidTank = TrackedTank(0, ::update)
    val links = Links()
    val powerCost: Int get() = (configuration.upgrades.getStackInSlot(0).count * (targetEnergy) / configuration.efficiencyModifier).roundToInt()
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
    private var soundTime = 0
    private var playingSound = false

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()
        if (!isCrafting)
            craft()
        else if (extractPower()) {
            updateProgress()
            tick++
            playSound()
        }
        finishRecipe()
    }

    /**
     * Play our sound if it's not playing
     */
    fun playSound() {
        //Finish our blocking of the sound playing after 30 seconds
        if (soundTime >= 20 * 35) {
            playingSound = false
            soundTime = -1
        }
        if (playingSound) soundTime++
        else soundTime = -1

        if (!playingSound) {
            if (Math.random() > 0.999) {
                playingSound = true
                level?.playSound(null, blockPos, Sounds.chant, BLOCKS, 0.5f, Math.random().toFloat())
            }
        }


    }

    private fun updateProgress() {
        if (progress.toInt() % 10 == 0) {
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

    private fun finishRecipe() {
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
    }

    private fun extractPower(): Boolean {
        val extracted = energy.extractEnergy(powerCost, true)
        if (extracted != powerCost) return false
        energy.extractEnergy(powerCost, false)
        return true
    }

    private fun craft() {
        val recipes = level?.recipeManager?.getAllRecipesFor(Registry.RecipeTypes.Synthesizer) ?: return
        val input = items.getStackInSlot(0)
        val tool = items.getStackInSlot(1)

        for (recipe in recipes) {
            if (recipe.ingredients[0].test(input) && recipe.ingredients[1].test(tool)) {
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
        tag.put("links", links.serializeNBT())
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
        links.deserializeNBT(tag.getCompound("links"))
        targetProgress = tag.getInt("target")
        targetEnergy = tag.getInt("targetEnergy")
        progress = tag.getFloat("progress")
        targetDamage = tag.getInt("damage")
        targetResult.deserializeNBT(tag.getCompound("result"))
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
        val menu = SimpleMenuProvider({ id, inv, _ -> SynthesizerContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

}