package com.github.sieves.content.modules

import com.github.sieves.api.ApiTab
import com.github.sieves.api.ApiTabItem
import com.github.sieves.api.tab.Tab
import com.github.sieves.api.tab.TabSpec
import com.github.sieves.content.io.fluids.FluidsTile
import com.github.sieves.registry.Registry
import com.github.sieves.util.*
import com.mojang.math.Vector3f
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.items.CapabilityItemHandler
import java.text.NumberFormat
import java.util.*
import kotlin.collections.HashMap

//class HungerModule : ApiTabItem("test".resLoc, BoxTile::class.java) {
class HungerModule : ApiTabItem(Registry.Tabs.PlayerHunger.key, FluidsTile::class.java) {
    /**
     * Adds some extra configurations
     */
    override fun configure(tab: ApiTab, level: Level, player: Player, blockPos: BlockPos, direction: Direction) {
        val tag = CompoundTag()
        tag.putBlockPos("linked_pos", blockPos)
        tag.putEnum("linked_face", direction)
        tab.setProperty("linked", tag)
    }

    companion object {
        private val cachedEntities = HashMap<UUID, BlockEntity>()

        internal val TagSpec = TabSpec()
            .withItem { ItemStack(Registry.Items.HungerModule) }
            .withTooltip { TranslatableComponent("tab.synth.player_hunger") }
            .withHover()
            .withSpin()
            .withTarget("net.minecraft.client.gui.screens.inventory.InventoryScreen")
            .withTarget("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen")
            .withInit(::init)
            .withServerTick(::onServerTick)
            .withServerClick(::onServerClick)
            .withMenu(::renderMenu)
            .build()

        private fun init(apiTab: ApiTab) {
            Log.info { "Initializing the player power tab!" }
        }

        private fun onServerClick(player: ServerPlayer, tab: ApiTab) {
            ApiTabItem.removeItem(player, tab)
        }

        private fun renderMenu(
            menuData: TabSpec.MenuData, player: Player, tab: ApiTab, containerIn: Any
        ) {
            val container = containerIn as net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>
            menuData.poseStack.translate(menuData.x.toDouble(), menuData.y.toDouble(), 0.0)

            container.drawTextShadow(menuData, 8f, 8f, "§nStats", 0xffffff)

            val linked = tab.getProperty("linked")
            if (linked.isPresent) {
//                menuData.poseStack.pushPose()
                menuData.poseStack.scale(0.5f, 0.5f, 0.5f)
                val pos = linked.get().getBlockPos("linked_pos")
                val be = cachedEntities[player.uuid] ?: player.level.getBlockEntity(pos) ?: return
                cachedEntities[player.uuid] = be
                val itemstack = ItemStack(player.level.getBlockState(pos).block)
                if (be !is FluidsTile) return
                val target = (20000 / be.getConfig().efficiencyModifier).toInt()
                container.drawTextShadow(
                    menuData,
                    12f,
                    50f,
                    "Uses: ${NumberFormat.getNumberInstance().format(target)}fe/feed",
                    0xffffff
                )
                container.drawTextShadow(menuData, 12f, 65f, "Pos: ${pos.toShortString()}", 0xffffff)
                container.drawTextShadow(menuData, 12f, 80f, "Block:", 0xffffff)
//                menuData.poseStack.popPose()
                (tab as Tab).renderItem(
                    menuData.x + 25f,
                    menuData.y + 42f, 1.5f,
                    Vector3f.YP.rotationDegrees(((System.currentTimeMillis() / 10) % 360).toFloat()),
                    itemstack,
                    container
                )
            }

        }

        private fun onServerTick(player: ServerPlayer, tab: ApiTab) {
            val linked = tab.getProperty("linked")
            linked.ifPresent {
                val pos = it.getBlockPos("linked_pos")
                val face = it.getEnum<Direction>("linked_face")
                val be = player.level.getBlockEntity(pos)
                var valid = true
                if (be is FluidsTile) {
                    val cap = be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                    val energyCap = be.getCapability(CapabilityEnergy.ENERGY)
                    if (!cap.isPresent || !energyCap.isPresent) valid = false
                    else {
                        val inv = cap.resolve().get()
                        val energy = energyCap.resolve().get()
                        for (slot in 0 until inv.slots) {
                            val item = inv.getStackInSlot(slot)
                            if (item.isEdible && player.foodData.needsFood()) {
                                val target = (20000 / be.getConfig().efficiencyModifier).toInt()
                                val extracted = energy.extractEnergy(target, true)
                                if (extracted == target) {
                                    val result = player.eat(player.level, item)
//                                    inv.extractItem(slot, 1, false)
                                    energy.extractEnergy(20000, true)
//                                    if (inv.getStackInSlot(slot).count > result.count)
//                                        inv.extractItem(slot, inv.getStackInSlot(slot).count - result.count, false)
                                    inv.extractItem(slot, 64, false)
                                    inv.insertItem(slot, result, false)
                                    break
                                }
                            }
                        }
                    }
                } else valid = false
                if (!valid) ApiTabItem.removeItem(player, tab)
            }
        }
    }
}
