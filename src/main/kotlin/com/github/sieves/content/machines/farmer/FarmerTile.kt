package com.github.sieves.content.machines.farmer

import com.github.sieves.api.ApiTile
import com.github.sieves.content.io.link.Links
import com.github.sieves.api.ApiConfig
import com.github.sieves.api.caps.*
import com.github.sieves.registry.Registry
import com.github.sieves.util.get
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.ItemTags
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.fluids.capability.templates.*
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.network.NetworkHooks
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class FarmerTile(pos: BlockPos, state: BlockState) :
    ApiTile<FarmerTile>(Registry.Tiles.Farmer, pos, state, "tile.synth.farmer"), Nameable {
    private val removals = ArrayList<BlockPos>()
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(21, ::update)
    override val fluids: FluidTank = TrackedTank(0, ::update)
    val powerCost: Int get() = ((links.getLinks().size * 1200) / configuration.efficiencyModifier).roundToInt()
    val sleepTime: Int get() = ((20 * 60) / configuration.speedModifier).roundToInt()
    override val ioPower: Int get() = ((links.getLinks().size * 1200) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)
    val links = Links()
    private var tick = 0

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()
        if (extractPower()) {
            if (tick >= 20) {
                linkCrops()
                purgeLinks()
                harvestCrops()
            }
            if (tick >= sleepTime) {
                growCrops()
                tick = 0
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
     * This will check the area surrounding a 3x3x3 for crops and link to them
     */
    private fun linkCrops() {
        for (x in -5 until 6) {
            for (y in -5 until 6) {
                for (z in -5 until 6) {
                    val pos = BlockPos(blockPos.x + x, blockPos.y + y, blockPos.z + z)
                    val state = level?.getBlockState(pos) ?: continue
                    when (state.block) {
                        is CropBlock -> links.addLink(pos, Direction.UP)
                        is GrassBlock -> links.addLink(pos, Direction.UP)
                        Blocks.DIRT -> links.addLink(pos, Direction.UP)
                    }
                }
            }
        }
    }

    /**
     * Removes links to invalid blocks
     */
    private fun purgeLinks() {
        removals.clear()
        for (link in links.getLinks()) {
            val state = level?.getBlockState(link.key) ?: continue
            val block = state.block
            if (block !is CropBlock && block != Blocks.GRASS_BLOCK && block != Blocks.DIRT) removals.add(link.key)
        }
        removals.forEach(links::removeLink)
    }

    /**
     * Grows one of the linked crops (1 per second)
     */
    private fun growCrops() {
        val hoeSlot = findDaHoe()
        for (link in links.getLinks()) {
            val state = level?.getBlockState(link.key) ?: continue
            val block = state.block
            if ((block == Blocks.GRASS_BLOCK || block == Blocks.DIRT) && hoeSlot != -1) {
                val hoe = items.extractItem(hoeSlot, 1, false)
                hoe.damageValue++
                level?.setBlockAndUpdate(link.key, Blocks.FARMLAND.defaultBlockState())
                if (hoe.damageValue < hoe.maxDamage)
                    items.insertItem(hoeSlot, hoe, false)
            }
            if (block !is CropBlock) continue
            block.performBonemeal(level as ServerLevel, level!!.random, link.key, state)
            if (!block.isMaxAge(state)) {
                Registry.Net.sendToClientsWithTileLoaded(Registry.Net.GrowBlock {
                    ownerPos = this@FarmerTile.blockPos
                    blockPos = link.key
                }, this)

            }
        }
    }

    private val hoeTag = ItemTags.create(ResourceLocation("minecraft", "hoes"))

    /**
     * Locates the hoe inside the inventory automatic hoeing     */
    private fun findDaHoe(): Int {
        for (slot in 0 until items.slots) {
            val stack = items[slot]
            if (stack.`is`(hoeTag)) {
                return slot
            }
        }
        return -1
    }

    /**
     * Harvests our crops
     */
    private fun harvestCrops() {
        var hasEmpty = false
        for (slot in 0 until items.slots) {
            val stack = items.getStackInSlot(slot)
            if (stack.isEmpty || stack.count < stack.item.getItemStackLimit(stack)) {
                hasEmpty = true
                break
            }
        }
        if (hasEmpty) for (link in links.getLinks()) {
            val state = level?.getBlockState(link.key) ?: continue
            val block = state.block
            if (block !is CropBlock || block is SaplingBlock) continue
            if (block.isMaxAge(state)) {
                val seed = block.getCloneItemStack(level!!, blockPos, state)
                val drops = Block.getDrops(state, level as ServerLevel, blockPos, null).filter { !it.sameItem(seed) }

                var missed = false
                drops.forEach {
                    val leftOver = ItemHandlerHelper.insertItem(items, it, false)
                    if (!leftOver.isEmpty) {
                        missed = true
                    }
                }
                if (!missed)
                    level?.setBlockAndUpdate(link.key, block.defaultBlockState())
                else break
                Registry.Net.sendToClientsWithTileLoaded(Registry.Net.HarvestBlock {
                    this.harvested.addAll(drops)
                    this.ownerPos = this@FarmerTile.blockPos
                    this.blockPos = link.key
                }, this)
            }
        }
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
        val menu = SimpleMenuProvider({ id, inv, _ -> FarmerContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

}