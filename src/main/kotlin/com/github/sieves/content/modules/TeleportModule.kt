package com.github.sieves.content.modules

import com.github.sieves.content.api.ApiTab
import com.github.sieves.content.api.ApiTabItem
import com.github.sieves.content.api.tab.Tab
import com.github.sieves.content.api.tab.TabSpec
import com.github.sieves.content.battery.BatteryTile
import com.github.sieves.registry.Registry
import com.github.sieves.util.*
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.energy.CapabilityEnergy
import java.text.NumberFormat
import java.util.UUID

class TeleportModule : ApiTabItem(Registry.Tabs.PlayerTeleport.key, BatteryTile::class.java) {
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
        private val teleports = HashMap<UUID, Pair<Vec3, Long>>()
        private val cachedEntities = HashMap<UUID, BlockEntity>()

        init {
            Registry.Net.Teleport.serverListener { teleportPacket, _ ->
                teleports[teleportPacket.playerUuid] = teleportPacket.toLocation to System.currentTimeMillis()
                true
            }

            Registry.Net.SoundEvent.clientListener { soundPacket, _ ->
                if (physicalClient)
                    Minecraft.getInstance().player?.playSound(soundPacket.sound, 0.5f, 1.0f)
                true
            }
        }

        internal val TabSpec = TabSpec().withItem { ItemStack(Registry.Items.TeleportModule) }
            .withTooltip { TranslatableComponent("tab.sieves.player_teleport") }.withHover().withSpin()
            .withTarget("net.minecraft.client.gui.screens.inventory.InventoryScreen")
            .withTarget("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen")
            .withInit(::init)
            .withServerTick(::onServerTick).withClientTick(::onClientTick).withServerClick(::onServerClick)
            .withMenu(::renderMenu).build()


        private fun init(apiTab: ApiTab) {
            Log.info { "Initializing the player power tab!" }
        }

        private fun onServerClick(player: ServerPlayer, tab: ApiTab) {
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
//                menuData.poseStack.pushPose()
                menuData.poseStack.scale(0.5f, 0.5f, 0.5f)
                val pos = linked.get().getBlockPos("linked_pos")
                val be = cachedEntities[player.uuid] ?: player.level.getBlockEntity(pos) ?: return
                cachedEntities[player.uuid] = be
                val itemstack = ItemStack(player.level.getBlockState(pos).block)
                if (be !is BatteryTile) return
                val target = (10000 / be.getConfig().efficiencyModifier).toInt()
                container.drawTextShadow(
                    menuData,
                    12f,
                    50f,
                    "Uses: ${NumberFormat.getNumberInstance().format(target)}fe/tp",
                    0xffffff
                )
                container.drawTextShadow(menuData, 12f, 65f, "Uses: 4 hunger/tp", 0xffffff)
                container.drawTextShadow(menuData, 12f, 80f, "Pos: ${pos.toShortString()}", 0xffffff)
                container.drawTextShadow(menuData, 12f, 95f, "Block:", 0xffffff)
//                menuData.poseStack.popPose()
                (tab as Tab).renderItem(
                    menuData.x + 25f,
                    menuData.y + 42f + 8f, 1.5f,
                    Vector3f.YP.rotationDegrees(((System.currentTimeMillis() / 10) % 360).toFloat()),
                    itemstack,
                    container
                )
            }

        }


        private fun onClientTick(player: Player, tab: ApiTab) {
            if (Registry.Keys.TeleportKey.isDown) {
                val location = player.rayTrace(250.0)
                val pos = location.blockPos
                val state = player.level.getBlockState(pos)
                if (!state.isAir && player.foodData.foodLevel > 0) {
                    Registry.Net.sendToServer(Registry.Net.Teleport {
                        playerUuid = player.uuid
                        toLocation = location.location
                    })
                }
            }
        }

        private fun onServerTick(player: ServerPlayer, tab: ApiTab) {
            val linked = tab.getProperty("linked")
            if (!teleports.containsKey(player.uuid)) return

            linked.ifPresent {
                val pos = it.getBlockPos("linked_pos")
                val face = it.getEnum<Direction>("linked_face")
                val be = player.level.getBlockEntity(pos)
                var valid = true
                if (be is BatteryTile) {
                    val cap = be.getCapability(CapabilityEnergy.ENERGY, face)
                    if (!cap.isPresent) valid = false
                    else {
                        val energy = cap.resolve().get()
                        val target = (10000 / be.getConfig().efficiencyModifier).toInt()
                        val extracted = energy.extractEnergy(target, true)
                        if (extracted == target && player.foodData.foodLevel > 0) {
                            val data = teleports.remove(player.uuid)!!
                            if (System.currentTimeMillis() - data.second < 1000) {
                                player.foodData.foodLevel -= 4
                                energy.extractEnergy(target, false)
                                val loc = data.first
                                if (player.isShiftKeyDown) {

                                    player.teleportTo(
                                        player.respawnPosition!!.x.toDouble(),
                                        player.respawnPosition!!.y + 1.0,
                                        player.respawnPosition!!.z.toDouble()
                                    )
                                } else
                                    player.teleportTo(loc.x, loc.y, loc.z)
                                Registry.Net.sendToClient(Registry.Net.SoundEvent {
                                    sound = SoundEvents.ENDERMAN_TELEPORT
                                }, player.uuid)
                            }
                        }
                    }
                } else valid = false
                if (!valid) removeItem(player, tab)
            }
        }
    }
}
