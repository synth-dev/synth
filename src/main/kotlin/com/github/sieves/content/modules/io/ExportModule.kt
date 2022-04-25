package com.github.sieves.content.modules.io

import com.github.sieves.api.ApiTab
import com.github.sieves.api.ApiTabItem
import com.github.sieves.api.tab.*
import com.github.sieves.content.io.box.*
import com.github.sieves.registry.Registry
import com.github.sieves.util.*
import com.mojang.math.Vector3f

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TextComponent
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.items.wrapper.InvWrapper
import net.minecraftforge.network.*
import java.text.NumberFormat
import java.util.UUID

//class SightModule : ApiTabItem("test".resLoc, BatteryTile::class.java) {
class ExportModule : ApiTabItem(Registry.Tabs.PlayerExport.key, BoxTile::class.java) {
    override fun onUse(player: ServerPlayer, itemStack: ItemStack): ItemStack {
        val capability = itemStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        if (capability.isPresent) {
            val menu =
                SimpleMenuProvider({ id, inv, _ -> FilterContainer(id, inv, itemStack) }, TextComponent("Export"))
            NetworkHooks.openGui(player, menu) { it.writeItem(itemStack) }
        }
        return itemStack
    }

    override fun initCapabilities(stack: ItemStack?, nbt: CompoundTag?): ICapabilityProvider? {
        if (stack == null) return super.initCapabilities(null, nbt)
        return FilterCapability.Provider(stack, 18)
    }

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
        itemStack.tag?.let { tab.setProperty("filter", it) }
    }

    companion object {
        private val cachedEntities = HashMap<UUID, BlockEntity>()

        internal val TabSpec = TabSpec().withItem { ItemStack(Registry.Items.ExportModule) }
            .withTooltip { TranslatableComponent("tab.synth.player_export") }.withHover().withSpin()
            .withTarget("net.minecraft.client.gui.screens.inventory.InventoryScreen")
            .withTarget("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen")
            .withInterval(5)
            .withServerTick(::onServerTick).withServerClick(::onServerClick).withMenu(::renderMenu).build()


        private fun onServerClick(player: ServerPlayer, tab: ApiTab) {
            removeItem(player, tab, tab.getProperty("filter").orElseGet { CompoundTag() })
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
                val be = cachedEntities[player.uuid] ?: player.level.getBlockEntity(pos) ?: return
                cachedEntities[player.uuid] = be
                val itemstack = ItemStack(player.level.getBlockState(pos).block)
                if (be !is BoxTile) return
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

                if (menuData.height < 71f) return

                val filter = tab.getProperty("filter")
                if (filter.isPresent) {
                    container.drawTextShadow(menuData, 12f, 95f, "Filter:", 0xffffff)
                    val tag = filter.get()
                    val size = tag.getInt("size")
                    for (i in 0 until size) {
                        if (tag.contains("slot_$i")) {
                            var x = menuData.x + 10 + i * 5.5f
                            var y = menuData.y + 50f + 6f
                            if (i >= 9) {
                                x = menuData.x + 10 + (i - 9) * 5.5f
                                y += 6
                            }
                            tab.renderItem(
                                x,
                                y,
                                0.8f,
                                Vector3f.YP.rotationDegrees((((System.currentTimeMillis()) / 15) % 360).toFloat()),
                                ItemStack.of(tag.getCompound("slot_$i")),
                                container
                            )
                        }
                    }
                }
            }

        }


        private fun onServerTick(player: ServerPlayer, tab: ApiTab) {
            if (!TabRegistry.hasTab(player.uuid, tab.key) || tab.getProperty("linked").isEmpty) {
//                removeItem(player, tab)
                return
            }
            val filter = tab.getProperty("filter")
            val linked = tab.getProperty("linked")

            linked.ifPresent {
                val pos = it.getBlockPos("linked_pos")
                val face = it.getEnum<Direction>("linked_face")
                val be = player.level.getBlockEntity(pos)
                var valid = true
                if (be is BoxTile) {
                    val itemCap = be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face)
                    val energyCap = be.getCapability(CapabilityEnergy.ENERGY, face)
                    if (!itemCap.isPresent || !energyCap.isPresent) valid = false
                    else {

                        val item = itemCap.resolve().get()
                        val energy = energyCap.resolve().get()
                        val target = (20000 / be.getConfig().efficiencyModifier).toInt()

                        val extracted = energy.extractEnergy(target, true)
                        if (extracted == target) {
                            val extracted =
                                if (filter.isPresent) extractFromInventoryFiltered(player, item, filter.get())
                                else extractFromInventory(player, item)
                            if (extracted) {
                                energy.extractEnergy(target, false)
                            }
                        } else {
                            //Do something if in valid extraction?
                        }
                    }
                } else valid = false
                if (!valid) {
                    removeItem(player, tab)

                }
            }
        }

        private fun extractFromInventory(from: ServerPlayer, into: IItemHandler): Boolean {
            //Extract unfiltered
            val inv = InvWrapper(from.inventory)
            var extracted = false
            for (slot in 0 until inv.slots) {
                val stack = inv.extractItem(slot, 4, true)
                val result = ItemHandlerHelper.insertItem(into, stack, true)
                if (result.isEmpty) {
                    inv.extractItem(slot, 4, false)
                    ItemHandlerHelper.insertItem(into, stack, false)
                    extracted = true
                }
            }
            return extracted
        }

        private fun extractFromInventoryFiltered(from: ServerPlayer, into: IItemHandler, filter: CompoundTag): Boolean {
            //
            val filters: MutableSet<ItemStack> = HashSet()
            for (i in 0 until filter.getInt("size")) {
                if (filter.contains("slot_$i")) {
                    filters.add(ItemStack.of(filter.getCompound("slot_$i")))
                }
            }
            var extracted = false

            val inv = InvWrapper(from.inventory)
            for (slot in 0 until inv.slots) {
                val stack = inv.extractItem(slot, 4, true)
                var valid = false
                for (itemFilter in filters) {
                    if (itemFilter.sameItem(stack)) {
                        valid = true
                        break
                    }
                }
                if (valid) {
                    val result = ItemHandlerHelper.insertItem(into, stack, true)
                    if (result.isEmpty) {
                        inv.extractItem(slot, 4, false)
                        ItemHandlerHelper.insertItem(into, stack, false)
                        extracted = true
                    }
                }
            }
            return extracted
        }

    }


}
