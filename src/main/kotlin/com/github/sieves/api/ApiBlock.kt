package com.github.sieves.api

import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Items
import com.github.sieves.registry.internal.net.ConfigurePacket
import com.github.sieves.registry.internal.net.TakeUpgradePacket
import com.github.sieves.util.Log
import com.github.sieves.util.getLevel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.items.wrapper.InvWrapper
import net.minecraftforge.network.NetworkEvent

abstract class ApiBlock<R : com.github.sieves.api.ApiTile<R>>(
    properties: Properties,
    private val type: () -> BlockEntityType<R>
) :
    Block(properties), EntityBlock {
    private val ticker: BlockEntityTicker<R> = com.github.sieves.api.ApiTile.Ticker()


    init {
        Registry.Net.TakeUpgrade.serverListener(::onTakeUpgrade)
        Registry.Net.Configure.serverListener(::onConfiguration)
    }


    private fun onConfiguration(configurePacket: ConfigurePacket, context: NetworkEvent.Context): Boolean {
        val level = configurePacket.getLevel(configurePacket.world)
        val blockEntity = level.getBlockEntity(configurePacket.blockPos)
        if (blockEntity !is ApiTile<*>) return false
        blockEntity.getConfig().deserializeNBT(configurePacket.config.serializeNBT())
        blockEntity.update()
        Log.info { "Updated configuration: $configurePacket" }
        return true
    }


    /**
     * Delegates our menu opening to our tile entity
     */
    override fun use(
        pState: BlockState, pLevel: Level, pPos: BlockPos, pPlayer: Player, pHand: InteractionHand, pHit: BlockHitResult
    ): InteractionResult {
        val hand = pPlayer.getItemInHand(InteractionHand.MAIN_HAND).item
        if (hand == Items.Linker || hand == Items.SpeedUpgrade || hand == Items.EfficiencyUpgrade) return InteractionResult.PASS
        if (!pLevel.isClientSide) {
            val tile = pLevel.getBlockEntity(pPos)
            if (tile is ApiTile<*>) tile.onMenu(pPlayer as ServerPlayer)
        }
        return InteractionResult.SUCCESS
    }

    protected fun onTakeUpgrade(configurePacket: TakeUpgradePacket, context: NetworkEvent.Context): Boolean {
        val inv = context.sender?.inventory ?: return false
        val level = context.sender!!.level
        val blockEntity = level.getBlockEntity(configurePacket.blockPos)
        if (blockEntity !is ApiTile<*>) return false
        val extracted = blockEntity.getConfig().upgrades.extractItem(configurePacket.slot, configurePacket.count, false)
        ItemHandlerHelper.insertItem(InvWrapper(inv), extracted, false)
        blockEntity.update()
        Log.info { "Updated configuration: $configurePacket" }
        return true
    }

    /**
     * This down casts our ticker to the correct type
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> {
        return ticker as BlockEntityTicker<T>
    }

    /**
     * Automatically creates our block entity for us
     */
    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity? = type().create(pPos, pState)
}