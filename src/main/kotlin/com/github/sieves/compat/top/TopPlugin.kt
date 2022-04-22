package com.github.sieves.compat.top

import com.github.sieves.content.api.ApiTile
import com.github.sieves.content.synthesizer.SynthesizerTile
import mcjty.theoneprobe.api.ITheOneProbe
import mcjty.theoneprobe.api.NumberFormat
import mcjty.theoneprobe.apiimpl.styles.TextStyle
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.item.ItemStack
import java.util.function.Function


class TopPlugin : Function<ITheOneProbe, Unit> {
    var formattedName: MutableComponent =
        TextComponent("sieves").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.ITALIC)

    override fun apply(t: ITheOneProbe) = with(t) {
        registerBlockDisplayOverride { probeMode, inf, player, level, blockState, data ->
            val tile = level.getBlockEntity(data.pos)
            if (tile is SynthesizerTile) {
                inf.horizontal()
                    .item(ItemStack(blockState.block.asItem()))
                    .vertical()
                    .text(TextComponent("§6Synthesizer"), TextStyle().topPadding(5))
                inf.text("power usage")
                    .text("${((tile.targetEnergy) / (tile.getConfig().efficiencyModifier)).toInt()}fe/tick")
                inf.text("total power usage")
                    .text(
                        "${
                            java.text.NumberFormat.getIntegerInstance()
                                .format(
                                    (tile.targetEnergy) / (tile.getConfig().efficiencyModifier)
                                        .toInt() * (tile.targetProgress / tile.getConfig().speedModifier)
                                )
                        }fe"
                    )
                val time = ((tile.targetProgress - (tile.progress)) / 20) / tile.getConfig().speedModifier
                val total = ((tile.targetProgress) / 20) / tile.getConfig().speedModifier

                inf.text("time")
                    .progress(time.toInt(), total.toInt())

                inf.horizontal()
                    .item(tile.items.getStackInSlot(0))
                    .text(" + ", TextStyle().topPadding(8))
                    .horizontal()
                    .item(tile.items.getStackInSlot(1))
                    .text(" → ", TextStyle().topPadding(8))
                    .horizontal()
                    .item(tile.items.getStackInSlot(2))
            }
            if (tile is ApiTile<*>) {
                inf.horizontal()
                    .text("speed → ", TextStyle().topPadding(8))
                    .item(tile.getConfig().upgrades.getStackInSlot(0))
                inf.horizontal()
                    .text("efficiency → ", TextStyle().topPadding(8))
                    .item(tile.getConfig().upgrades.getStackInSlot(1))
            }
            true
        }
    }
}