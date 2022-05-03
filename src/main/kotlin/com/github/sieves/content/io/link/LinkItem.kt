package com.github.sieves.content.io.link

import com.github.sieves.api.*
import com.github.sieves.registry.Registry
import com.github.sieves.dsl.*
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.InteractionResult
import net.minecraft.world.Nameable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.items.CapabilityItemHandler

class LinkItem : Item(Properties().tab(Registry.Items.CreativeTab).stacksTo(1).durability(1000)) {

    override fun isFoil(pStack: ItemStack): Boolean {
        return pStack.tag?.getBoolean("foil") == true
    }

    /**
     * Called when this item is used when targetting a Block
     */
    override fun useOn(pContext: UseOnContext): InteractionResult {
        if (pContext.level.isClientSide) return InteractionResult.SUCCESS
        val item = pContext.itemInHand
        val tag = item.orCreateTag
        val level = pContext.level
        val tile = level.getBlockEntity(pContext.clickedPos)
        if (pContext.player?.isShiftKeyDown == true && tile == null) {
            if (tag.contains("from")) {
                val from = tag.getBlockPos("from")
                val block = level.getBlockState(from.bp)

                tag.remove("hasFrom")
                pContext.player?.displayClientMessage(
                    TextComponent("removed → ${block.block.name.string}"), false
                )
                tag.remove("from")
            }
            tag.putBoolean("foil", false)
        } else {
            if (pContext.player?.isShiftKeyDown == true && (tile is ApiLinkable)) {
                if (tag.contains("hasFrom")) {
                    val from = tag.getBlockPos("from")
                    val fromTile = level.getBlockEntity(from.bp)
                    if (fromTile !is ApiLinkable) return InteractionResult.FAIL
                    if (from == tile.blockPos) return InteractionResult.FAIL
                    val to = level.getBlockEntity(pContext.clickedPos) ?: return InteractionResult.FAIL
                    if (!to.getCapability(
                            CapabilityEnergy.ENERGY, pContext.clickedFace
                        ).isPresent && !to.getCapability(
                            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, pContext.clickedFace
                        ).isPresent
                    ) return InteractionResult.FAIL
                    fromTile.linkTo(pContext.clickedPos, pContext.clickedFace)
                    tag.remove("hasFrom")
                    tag.remove("from")
                    pContext.player?.displayClientMessage(
                        TextComponent("linked → ${(fromTile as Nameable).name.string} → ${tile.blockState.block.name.string}"),
                        false
                    )
                    tag.putBoolean("foil", false)
                } else {
                    tile.unlink()
                    val from = tag.getBlockPos("from")
                    val block = level.getBlockState(from.bp)
                    pContext.player?.displayClientMessage(
                        TextComponent("removed → ${block.block.name.string}"), false
                    )
                    tag.remove("hasFrom")
                    tag.remove("from")
                }

            } else if (tag.getBoolean("hasFrom")) {
                val from = tag.getBlockPos("from")
                val fromTile = level.getBlockEntity(from.bp)
                if (fromTile !is ApiLinkable) return InteractionResult.FAIL
                if (from == tile!!.blockPos) return InteractionResult.FAIL
                val to = level.getBlockEntity(pContext.clickedPos) ?: return InteractionResult.FAIL
                if (!to.getCapability(
                        CapabilityEnergy.ENERGY, pContext.clickedFace
                    ).isPresent && !to.getCapability(
                        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, pContext.clickedFace
                    ).isPresent && !to.getCapability(
                        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, pContext.clickedFace
                    ).isPresent
                ) return InteractionResult.FAIL
                fromTile.linkTo(pContext.clickedPos, pContext.clickedFace)
                tag.remove("hasFrom")
                tag.remove("from")
                pContext.player?.displayClientMessage(
                    TextComponent("linked → ${(fromTile as Nameable).name.string} → ${tile.blockState.block.name.string}"),
                    false
                )
                tag.putBoolean("foil", false)
            } else {
                if (tile !is ApiLinkable) return InteractionResult.PASS
                tag.putBoolean("hasFrom", true)
                tag.putBlockPos("from", tile.blockPos)
                pContext.player?.displayClientMessage(
                    TextComponent("started → ${(tile as Nameable).name.string}"),
                    false
                )
                tag.putBoolean("foil", true)
            }
        }
        item.tag = tag
        return InteractionResult.SUCCESS
    }

    override fun isEnchantable(pStack: ItemStack): Boolean {
        return true
    }

    /**
     * Return the enchantability factor of the item, most of the time is based on material.
     */
    override fun getEnchantmentValue(): Int {
        return 1
    }
}