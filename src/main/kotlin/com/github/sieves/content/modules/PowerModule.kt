package com.github.sieves.content.modules

import com.github.sieves.api.ApiTab
import com.github.sieves.api.ApiTabItem
import com.github.sieves.api.tab.Tab
import com.github.sieves.api.tab.TabSpec
import com.github.sieves.content.io.battery.BatteryTile
import com.github.sieves.registry.Registry
import com.github.sieves.dsl.*
import com.mojang.math.Vector3f
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.energy.CapabilityEnergy
import java.text.NumberFormat
import java.util.*
import kotlin.collections.HashMap

//class PowerModule : ApiTabItem("test".resLoc, BatteryTile::class.java) {
class PowerModule : ApiTabItem(Registry.Tabs.PlayerPower.key, BatteryTile::class.java) {
    /**
     * Adds some extra configurations
     */
    override fun configure(tab: ApiTab, level: Level, player: Player, blockPos: BlockPos, direction: Direction, itemStack: ItemStack) {
        val tag = CompoundTag()
        tag.putBlockPos("linked_pos", blockPos)
        tag.putEnum("linked_face", direction)
        tab.setProperty("linked", tag)
    }

    companion object {
        private val cachedEntities = HashMap<UUID, BlockEntity>()

        /**
         * This tab is used to send out power from a linked location
         */
        val TagSpec = TabSpec()
            .withItem { ItemStack(Registry.Items.PowerModule) }
            .withTooltip { TranslatableComponent("tab.synth.player_power") }
            .withHover()
            .withSpin()
            .withTarget("net.minecraft.client.gui.screens.inventory.InventoryScreen")
            .withTarget("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen")
            .withServerTick(::onServerTick)
            .withServerClick(::onServerClick)
            .withMenu(::renderMenu)
            .build()

        private fun onServerClick(player: ServerPlayer, tab: ApiTab) {
            ApiTabItem.removeItem(player, tab)
        }

        private fun renderMenu(
            menuData: TabSpec.MenuData, player: Player, tab: ApiTab, containerIn: Any,
        ) {
            val container = containerIn as net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>
            menuData.poseStack.translate(menuData.x.toDouble(), menuData.y.toDouble(), 0.0)

            container.drawTextShadow(menuData, 8f, 8f, "§nStats", 0xffffff)

            val linked = tab.getProperty("linked")
            if (linked.isPresent) {
//                menuData.poseStack.pushPose()
                menuData.poseStack.scale(0.5f, 0.5f, 0.5f)
                val pos = linked.get().getBlockPos("linked_pos")
                val be = cachedEntities[player.uuid] ?: player.level.getBlockEntity(pos.bp) ?: return
                cachedEntities[player.uuid] = be
                val itemstack = ItemStack(player.level.getBlockState(pos.bp).block)
                if (be !is BatteryTile) return
                val target = (10000 / be.getConfig().efficiencyModifier).toInt()
                container.drawTextShadow(
                    menuData,
                    12f,
                    50f,
                    "Uses: ${NumberFormat.getNumberInstance().format(target)}fe/tick",
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
            if (linked.isPresent) {
                val it = linked.get()
                val pos = it.getBlockPos("linked_pos")
                val be = player.level.getBlockEntity(pos.bp)
                var valid = true
                if (be is BatteryTile) {
                    val cap = be.getCapability(CapabilityEnergy.ENERGY)
                    if (!cap.isPresent) valid = false
                    else {
                        val energy = cap.resolve().get()
                        val inv = player.inventory
                        val energize: (NonNullList<ItemStack>) -> Unit = { items ->
                            for (slot in items) {
                                slot.getCapability(CapabilityEnergy.ENERGY).ifPresent { otherEnergy ->
                                    val extracted =
                                        energy.extractEnergy((2000 * be.getConfig().speedModifier).toInt(), true)
                                    val recieved = otherEnergy.receiveEnergy(extracted, false)
                                    energy.extractEnergy(recieved, false)
                                }
                            }
                        }
                        energize(inv.items)
                        energize(inv.armor)
                        energize(inv.offhand)
                    }
                } else {
                    valid = false
                }
                if (!valid) {
//                    tab.removeProperty("linked")
//                    tab.syncToClients()
                    ApiTabItem.removeItem(player, tab)
                }
//                println("Found link: ${pos.toShortString()}, ${face.getName()}")
            }
        }
    }
}
