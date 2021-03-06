package com.github.sieves.content.machines.forester

import com.github.sieves.api.ApiTile
import com.github.sieves.content.io.link.Links
import com.github.sieves.api.ApiConfig
import com.github.sieves.api.caps.*
import com.github.sieves.registry.Registry
import com.github.sieves.dsl.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
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

class ForesterTile(pos: BlockPos, state: BlockState) :
    ApiTile<ForesterTile>(Registry.Tiles.Forester, pos, state, "tile.synth.forester"), Nameable {
    private val removals = ArrayList<BlockPos>()
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(21, ::update)
    override val tank: FluidTank = TrackedTank(0, ::update)
    val powerCost: Int get() = ((links.getLinks().size * 1200) / configuration.efficiencyModifier).roundToInt()
    val sleepTime: Int get() = ((20 * 120) / configuration.speedModifier).roundToInt()
    override val ioPower: Int get() = ((links.getLinks().size * 1200) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)
    private val savedSaplings = HashMap<BlockPos, Block>()
    private var tick = 0
    val links = Links()

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
            tick++
        }
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
        for (x in -15 until 16) {
            for (y in -15 until 50) {
                for (z in -15 until 16) {
                    val pos = BlockPos(blockPos.x + x, blockPos.y + y, blockPos.z + z)
                    val state = level?.getBlockState(pos) ?: continue
                    val block = state.block
                    if (block is SaplingBlock) {
                        links.add(pos, Direction.UP)
                        savedSaplings[pos] = block
                    }
                    if (state.`is`(BlockTags.LOGS)) links.add(pos, Direction.UP)
                    if (state.`is`(BlockTags.LEAVES)) links.add(pos, Direction.UP)
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
            if (block !is SaplingBlock && !state.`is`(BlockTags.LOGS) && !state.`is`(BlockTags.LEAVES)) removals.add(
                link.key
            )
        }
        removals.forEach(links::remove)
    }

    /**
     * Grows one of the linked crops (1 per second)
     */
    private fun growCrops() {
        for (link in links.getLinks()) {
            val state = level?.getBlockState(link.key) ?: continue
            val block = state.block
            if (block !is SaplingBlock) continue
            block.performBonemeal(level as ServerLevel, level!!.random, link.key, state)
            Registry.Net.sendToClientsWithTileLoaded(Registry.Net.GrowBlock {
                ownerPos = this@ForesterTile.blockPos
                blockPos = link.key
                isFarmer = false
            }, this)
        }
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
            if (!state.`is`(BlockTags.LOGS) && !state.`is`(BlockTags.LEAVES)) continue
            val drops = Block.getDrops(state, level as ServerLevel, blockPos, null)

            var removed = false
            savedSaplings.remove(link.key)?.let {
                level?.setBlockAndUpdate(link.key, it.defaultBlockState())
                removed = true
            }
            var missed = false
            drops.forEach {
                val leftOver = ItemHandlerHelper.insertItem(items, it, false)
                if (!leftOver.isEmpty) {
                    missed = true
                }
            }
            if (!missed && !removed)
                level?.setBlockAndUpdate(link.key, Blocks.AIR.defaultBlockState())
            else break
            Registry.Net.sendToClientsWithTileLoaded(Registry.Net.HarvestBlock {
                this.harvested.addAll(drops)
                this.ownerPos = this@ForesterTile.blockPos
                this.blockPos = link.key
                isFarmer = false
            }, this)
        }

    }

    /**
     * Called when saving nbt data
     */
    override fun onSave(tag: CompoundTag) {
        tag.put("links", links.serializeNBT())
        tag.put("sapling", this.savedSaplings.toCompound())
    }

    /**
     * Called when loading the nbt data
     */
    override fun onLoad(tag: CompoundTag) {
        links.deserializeNBT(tag("links"))
        val map: Map<BlockPos, Block> = tag("sapling").toMap()
        this.savedSaplings.putAll(map)
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
        val menu = SimpleMenuProvider({ id, inv, _ -> ForesterContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

}