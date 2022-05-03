package com.github.sieves.content.io.fluids

import com.github.sieves.registry.Registry
import com.github.sieves.dsl.*
import com.github.sieves.dsl.Log.info
import net.minecraft.world.*
import net.minecraft.world.InteractionResult.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*

/**
 * Our item that will be displayed in game
 */
class FluidsItem : BlockItem(
    Registry.Blocks.Fluids,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(1).fireResistant()


) {


    override fun use(pLevel: Level, pPlayer: Player, pUsedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val ray = pPlayer.rayTrace(8.0)
        val pos = ray.blockPos.offset(0, -1, 0)
        val state = pLevel.getBlockState(pos)
        val fluidState = pLevel.getFluidState(pos)
        if (!fluidState.isEmpty) {
            val context = UseOnContext(pPlayer, pUsedHand, ray)
            val result = place(BlockPlaceContext(context))
            if (result == sidedSuccess(pLevel.isClientSide)) {
                var stack = pPlayer.getItemInHand(pUsedHand).copy()
                stack.shrink(1)
                if (stack.count <= 0) stack = ItemStack.EMPTY
                info { "${ray.blockPos}, ${state.block.name.string}" }
                return InteractionResultHolder.sidedSuccess(stack, pLevel.isClientSide)
            }
        }
        return super.use(pLevel, pPlayer, pUsedHand)
    }


    /**
     * Called when this item is used when targeting a Block
     */
    override fun useOn(pContext: UseOnContext): InteractionResult {
        val ray = pContext.player?.rayTrace(8.0) ?: return super.useOn(pContext)
        val pos = ray.blockPos.offset(0, -1, 0) ?: return super.useOn(pContext)
        val fluidState = pContext.level.getFluidState(pos)
        if (!fluidState.isEmpty) {
            val context = UseOnContext(pContext.player!!, pContext.hand, ray)
            return super.useOn(context)
        }
        return super.useOn(pContext)
    }
}