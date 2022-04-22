package com.github.sieves.content.forester

import com.github.sieves.Sieves
import com.github.sieves.content.api.ApiTile
import com.github.sieves.content.api.caps.TrackedEnergy
import com.github.sieves.content.api.caps.TrackedInventory
import com.github.sieves.content.link.Links
import com.github.sieves.content.tile.internal.Configuration
import com.github.sieves.registry.Registry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.item.BlockItem
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

class ForesterTile(pos: BlockPos, state: BlockState) :
    ApiTile<ForesterTile>(Registry.Tiles.Forester, pos, state, "tile.sieves.forester"), Nameable {
    private val energyHandler = LazyOptional.of { energy }
    private val itemHandler = LazyOptional.of { items }
    private var tick = 0
    private val removals = ArrayList<BlockPos>()
    override val energy = TrackedEnergy(250_000, ::update)
    override val items = TrackedInventory(21, ::update)
    val links = Links()
    val powerCost: Int get() = ((links.getLinks().size * 1200) / configuration.efficiencyModifier).roundToInt()
    val sleepTime: Int get() = ((20 * 120) / configuration.speedModifier).roundToInt()
    override val ioPower: Int get() = ((links.getLinks().size * 1200) * configuration.efficiencyModifier).roundToInt()
    override val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)
    private val savedSaplings = HashMap<BlockPos, Block>()

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
        for (x in -15 until 15) {
            for (y in -15 until 50) {
                for (z in -15 until 15) {
                    val pos = BlockPos(blockPos.x + x, blockPos.y + y, blockPos.z + z)
                    val state = level?.getBlockState(pos) ?: continue
                    val block = state.block
                    if (block is SaplingBlock) {
                        links.addLink(pos, Direction.UP)
                        savedSaplings[pos] = block
                    }
                    if (state.`is`(BlockTags.LOGS)) links.addLink(pos, Direction.UP)
                    if (state.`is`(BlockTags.LEAVES)) links.addLink(pos, Direction.UP)
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
        removals.forEach(links::removeLink)
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
            val block = state.block
            val seed = block.getCloneItemStack(level!!, blockPos, state)

            if (!state.`is`(BlockTags.LOGS) && !state.`is`(BlockTags.LEAVES)) continue
            val drops = Block.getDrops(state, level as ServerLevel, blockPos, null)
//            val seed =  Block.getDrops(state, level as ServerLevel, blockPos, null).filter { (it.item is BlockItem && (it.item as BlockItem).block) }


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
            }, this)
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
    }

    /**
     * Called when loading the nbt data
     */
    override fun onLoad(tag: CompoundTag) {
        energy.deserializeNBT(tag.get("energy"))
        links.deserializeNBT(tag.getCompound("links"))
        items.deserializeNBT(tag.getCompound("items"))
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
        val menu = SimpleMenuProvider({ id, inv, _ -> ForesterContainer(id, inv, blockPos, this) }, name)
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
        return TranslatableComponent("container.${Sieves.ModId}.forester")
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