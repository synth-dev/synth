package com.github.sieves.content.modules

import com.github.sieves.api.ApiTab
import com.github.sieves.api.ApiTabItem
import com.github.sieves.api.tab.*
import com.github.sieves.content.io.battery.BatteryTile
import com.github.sieves.registry.Registry
import com.github.sieves.dsl.*
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft

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
import java.text.NumberFormat
import java.util.UUID

//class SightModule : ApiTabItem("test".resLoc, BatteryTile::class.java) {
class FlightModule : ApiTabItem(Registry.Tabs.PlayerFlight.key, BatteryTile::class.java) {
    /**
     * Adds some extra configurations
     */
    override fun configure(
        tab: ApiTab,
        level: Level,
        player: Player,
        blockPos: BlockPos,
        direction: Direction,
        itemStack: ItemStack
    ) {
        val tag = CompoundTag()
        tag.putBlockPos("linked_pos", blockPos)
        tag.putEnum("linked_face", direction)
        tab.setProperty("linked", tag)
    }

    companion object {
        private val cachedEntities = HashMap<UUID, BlockEntity>()

        init {
            if (physicalClient) Registry.Net.FlightToggle.clientListener { packet, _ ->
                val player = Minecraft.getInstance().player
                if (player?.uuid == packet.uuid) {
                    player.abilities.mayfly = packet.enabled
                    if (!packet.enabled) player.abilities.flying = false
                }
                true
            }
        }

        internal val TabSpec = TabSpec().withItem { ItemStack(Registry.Items.FlightModule) }
            .withTooltip { TranslatableComponent("tab.synth.player_flight") }.withHover().withSpin()
            .withTarget("net.minecraft.client.gui.screens.inventory.InventoryScreen")
            .withTarget("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen")
            .withInterval(20)
            .withServerTick(::onServerTick).withServerClick(::onServerClick).withMenu(::renderMenu).build()


        private fun onServerClick(player: ServerPlayer, tab: ApiTab) {
            player.abilities.mayfly = false
            player.onUpdateAbilities()
            Registry.Net.sendToClient(Registry.Net.FlightToggle {
                enabled = false
                uuid = player.uuid
            }, player.uuid)
            removeItem(player, tab)
        }

        private fun renderMenu(
            menuData: TabSpec.MenuData, player: Player, tab: ApiTab, containerIn: Any
        ) {
            val container = containerIn as net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>
            menuData.poseStack.translate(menuData.x.toDouble(), menuData.y.toDouble(), 0.0)

            container.drawTextShadow(menuData, 8f, 8f, "§nStats", 0xffffff)

            val linked = tab.getProperty("linked")
            if (linked.isPresent) {
                menuData.poseStack.scale(0.5f, 0.5f, 0.5f)
                val pos = linked.get().getBlockPos("linked_pos")
                val be = cachedEntities[player.uuid] ?: player.level.getBlockEntity(pos.bp) ?: return
                cachedEntities[player.uuid] = be
                val itemstack = ItemStack(player.level.getBlockState(pos.bp).block)
                if (be !is BatteryTile) return
                val target = (2500 / be.getConfig().efficiencyModifier).toInt()
                container.drawTextShadow(
                    menuData, 12f, 50f, "Uses: ${NumberFormat.getNumberInstance().format(target)}fe/tick", 0xffffff
                )
                container.drawTextShadow(menuData, 12f, 65f, "Pos: ${pos.toShortString()}", 0xffffff)
                container.drawTextShadow(menuData, 12f, 80f, "Block:", 0xffffff)
                (tab as Tab).renderItem(
                    menuData.x + 25f,
                    menuData.y + 34f + 8f,
                    1.5f,
                    Vector3f.YP.rotationDegrees(((System.currentTimeMillis() / 10) % 360).toFloat()),
                    itemstack,
                    container
                )
            }

        }


        private fun onServerTick(player: ServerPlayer, tab: ApiTab) {
            if (!TabRegistry.hasTab(player.uuid, tab.key) || tab.getProperty("linked").isEmpty) {
                player.onUpdateAbilities()
                player.abilities.mayfly = true
                Registry.Net.sendToClient(Registry.Net.FlightToggle {
                    enabled = false
                    uuid = player.uuid
                }, player.uuid)
                return
            }

            val linked = tab.getProperty("linked")
            linked.ifPresent {
                val pos = it.getBlockPos("linked_pos")
                val face = it.getEnum<Direction>("linked_face")
                val be = player.level.getBlockEntity(pos.bp)
                var valid = true
                if (be is BatteryTile) {
                    val cap = be.getCapability(CapabilityEnergy.ENERGY)
                    if (!cap.isPresent) valid = false
                    else {
                        val energy = cap.resolve().get()
                        val target = (20000 / be.getConfig().efficiencyModifier).toInt()
                        val extracted = energy.extractEnergy(target, true)
                        if (extracted == target) {
                            player.onUpdateAbilities()
                            player.abilities.mayfly = true
                            energy.extractEnergy(target, false)
                            Registry.Net.sendToClient(Registry.Net.FlightToggle {
                                enabled = true
                                uuid = player.uuid
                            }, player.uuid)
                        } else {
                            player.abilities.mayfly = false
                            player.onUpdateAbilities()
                            Registry.Net.sendToClient(Registry.Net.FlightToggle {
                                enabled = false
                                uuid = player.uuid

                            }, player.uuid)
                        }
                    }
                } else valid = false
                if (!valid) {
                    removeItem(player, tab)
                    player.abilities.mayfly = false
                    player.onUpdateAbilities()
                    Registry.Net.sendToClient(Registry.Net.FlightToggle {
                        enabled = false
                        uuid = player.uuid
                    }, player.uuid)
                }
            }
        }
    }
}
