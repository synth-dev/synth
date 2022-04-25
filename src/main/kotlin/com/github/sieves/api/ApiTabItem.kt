package com.github.sieves.api

import com.github.sieves.api.tab.TabRegistry
import com.github.sieves.registry.Registry
import com.github.sieves.util.rayTrace
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.items.wrapper.InvWrapper

abstract class ApiTabItem(
    protected val tabKey: ResourceLocation, protected vararg val target: Class<out BlockEntity>
) : Item(Properties().stacksTo(1).fireResistant().tab(Registry.Items.CreativeTab)) {

    override fun isFoil(pStack: ItemStack): Boolean {
        return true
    }

    protected open fun onUse(player: ServerPlayer, itemStack: ItemStack): ItemStack {
        return itemStack
    }

    override fun use(pLevel: Level, pPlayer: Player, pUsedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val result = pPlayer.rayTrace(8.0)
        val pos = result.blockPos
        val be = pLevel.getBlockEntity(pos)
        var valid = false
        for (target in this.target) {
            if (target.isInstance(be)) {
                valid = true
                break
            }
        }
        val itemstack = if (pLevel.isClientSide) pPlayer.getItemInHand(pUsedHand) else if (!valid) onUse(
            pPlayer as ServerPlayer,
            pPlayer.getItemInHand(pUsedHand)
        ) else pPlayer.getItemInHand(pUsedHand)
        if(pLevel.isClientSide) return InteractionResultHolder.success(itemstack)
        if (valid) {
            //This will be sent/registered to the client
            if (!TabRegistry.hasTab(pPlayer.uuid, tabKey)) {
                if (TabRegistry.createAndBind(pPlayer.uuid, tabKey) {
                        configure(it, pLevel, pPlayer, result.blockPos, result.direction, itemstack)
                    }) {
//                    pPlayer.inventory.setItem(pPlayer.inventory.selected, ItemStack.EMPTY)
                    pPlayer.inventory.removeFromSelected(true)
                    return InteractionResultHolder.consume(ItemStack.EMPTY)
                }
            }
        }
        return InteractionResultHolder.fail(itemstack)
    }

    /**
     * Adds some extra configurations. This is called on the both the client and server, configuration is required on both sides
     */
    protected open fun configure(
        tab: ApiTab,
        level: Level,
        player: Player,
        hit: BlockPos,
        face: Direction,
        itemStack: ItemStack
    ) = Unit

    companion object
}