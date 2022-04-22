package com.github.sieves.content.api

import com.github.sieves.content.api.tab.TabRegistry
import com.github.sieves.registry.Registry
import com.github.sieves.util.rayTrace
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
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
//    override fun useOn(pContext: UseOnContext): InteractionResult {
//        val pos = pContext.clickedPos
//        val be = pContext.level.getBlockEntity(pos)
//        var valid = false
//        for (target in this.target) {
//            if (target.isInstance(be)) {
//                valid = true
//                break
//            }
//        }
//        if (valid && pContext.player != null) {
//            //This will be sent/registered to the client
//            if (!TabRegistry.hasTab(pContext.player!!.uuid, tabKey)) {
//                if (TabRegistry.createAndBind(pContext.player!!.uuid, tabKey) {
//                        configure(it, pContext.level, pContext.player!!, pContext.clickedPos, pContext.clickedFace)
//                    }) {
//                    val inv = pContext.player!!.inventory
//                    if (!pContext.level.isClientSide) {
//                        inv.setItem(inv.selected, ItemStack.EMPTY)
//                        inv.setChanged()
//                    }
////                    InvWrapper(inv).extractItem(inv.selected, 1, false)
//                    return InteractionResult.sidedSuccess(pContext.level.isClientSide)
//                }
//            }
//        }
//        return super.useOn(pContext)
//    }
    override fun isFoil(pStack: ItemStack): Boolean {
        return true
    }

    override fun use(pLevel: Level, pPlayer: Player, pUsedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val result = pPlayer.rayTrace(8.0)
        val pos = result.blockPos
        val be = pLevel.getBlockEntity(pos)
        var valid = false
//        if (TabRegistry.hasTab(pPlayer.uuid, tabKey)) return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand))
        for (target in this.target) {
            if (target.isInstance(be)) {
                valid = true
                break
            }
        }
        if (valid) {
            //This will be sent/registered to the client
            if (!TabRegistry.hasTab(pPlayer.uuid, tabKey)) {
                if (TabRegistry.createAndBind(pPlayer.uuid, tabKey) {
                        configure(it, pLevel, pPlayer, result.blockPos, result.direction)
                    }) {
//                    pPlayer.inventory.setItem(pPlayer.inventory.selected, ItemStack.EMPTY)
                    if (pLevel.isClientSide) InteractionResultHolder.success(ItemStack.EMPTY)
                    return InteractionResultHolder.consume(ItemStack.EMPTY)
                }
            }
        }
        return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand))
    }

    /**
     * Adds some extra configurations. This is called on the both the client and server, configuration is required on both sides
     */
    protected open fun configure(tab: ApiTab, level: Level, player: Player, hit: BlockPos, face: Direction) = Unit

    companion object
}