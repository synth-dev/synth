package com.github.sieves.content.upgrade

import com.github.sieves.content.api.ApiItem
import com.github.sieves.content.api.ApiTile
import com.github.sieves.util.rayTrace
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class Upgrade(private val slot: Int, maxStack: Int) : ApiItem(maxStack) {
    override fun use(pLevel: Level, pPlayer: Player, pUsedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val item = pPlayer.getItemInHand(pUsedHand).copy()
        val tile = pLevel.getBlockEntity(pPlayer.rayTrace(8.0).blockPos)
        if (tile is ApiTile<*>) {
            val leftOver = tile.getConfig().upgrades.insertItem(slot, ItemStack(item.item), pLevel.isClientSide)
            if (leftOver == ItemStack.EMPTY) {
                item.shrink(1)
                pPlayer.setItemInHand(pUsedHand, item)
                return InteractionResultHolder.success(item)
            }
        }
        return super.use(pLevel, pPlayer, pUsedHand)
    }

}