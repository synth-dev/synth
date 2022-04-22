package com.github.sieves.content.synthesizer

import com.github.sieves.content.api.ApiScreen
import com.github.sieves.content.api.gui.ConfigWidget
import com.github.sieves.content.api.gui.UpgradesWidget
import com.github.sieves.content.tile.internal.Configuration
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.energy.CapabilityEnergy
import java.text.NumberFormat
import kotlin.math.roundToInt

@Suppress("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES")
class SynthesizerScreen(
    val container: SynthesizerContainer, playerInv: Inventory
) : ApiScreen<SynthesizerContainer, SynthesizerTile>(container, playerInv), ConfigWidget<SynthesizerTile>,
    UpgradesWidget<SynthesizerTile> {
    override val texture: ResourceLocation = "textures/gui/sieve_gui.png".resLoc
    override fun renderMain(stack: PoseStack, mouseX: Double, mouseY: Double) {
        blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight)
    }

    override fun renderOverlayWidgets(stack: PoseStack, mouseX: Double, mouseY: Double) {
        renderCharger(stack, mouseX, mouseY)
        val power = (tile().energy.energyStored / tile().energy.maxEnergyStored.toFloat()) * 68
        blit(stack, this.leftPos + 165, this.topPos + 8, 0, 114, 3, power.toInt())

        val percent = ((tile().progress / tile().targetProgress.toFloat())) * 20f
        blit(stack, this.leftPos + 90, this.topPos + 35, 0, 99, percent.toInt(), 15)
    }

    override fun renderToolTips(stack: PoseStack, mouseX: Double, mouseY: Double) {
        if (isHovered(163, 8, 5, 67, mouseX, mouseY)) {
            if (hasShiftDown()) {
                drawTooltip(
                    stack, "using: §6${
                        NumberFormat.getIntegerInstance()
                            .format(((tile().targetEnergy) / (tile().getConfig().efficiencyModifier).roundToInt()))
                    }FE/t", mouseX, mouseY
                )
            } else
                drawTooltip(
                    stack, "power: §6${
                        NumberFormat.getIntegerInstance().format(tile().energy.energyStored)
                    }FE/${NumberFormat.getIntegerInstance().format(tile().energy.maxEnergyStored)}", mouseX, mouseY
                )
        }

        if (isHovering(
                90,
                35,
                25,
                15,
                mouseX,
                mouseY
            )
        ) {
            val progress = ((tile().progress) / tile().targetProgress.toFloat()) * 100
            val time = ((tile().targetProgress - (tile().progress)) / 20) / tile().getConfig().speedModifier
            if (!hasShiftDown())
                drawTooltip(stack, "progress: §6${progress.toInt()}%", mouseX, mouseY)
            else
                drawTooltip(stack, "time left: §6${time.toInt()}s", mouseX, mouseY)

        }
    }

    private fun renderCharger(stack: PoseStack, mouseX: Double, mouseY: Double) {
        val item = tile().items.getStackInSlot(0)
        if (item.isEmpty) return
        val cap = item.getCapability(CapabilityEnergy.ENERGY)
        if (cap.isPresent) {
            val energy = cap.resolve().get()
            val percent = (energy.energyStored / energy.maxEnergyStored.toFloat()) * 11f
            blit(stack, this.leftPos + 83, this.topPos + 56, 59, 0, 9, percent.toInt())
        }
    }


    //ConfigWidget
    override val config: Configuration get() = container.tile.getConfig()
    override var configWidth: Float = 0f
    override var configure: Boolean = false

    /**
     * Pass a reference to our tile
     */
    override val tile: () -> SynthesizerTile
        get() = { container.tile }

    /**
     * This is called by the basecontainer screen
     */
    override fun preRender(stack: PoseStack, mouseX: Double, mouseY: Double) {
        super<ConfigWidget>.preRender(stack, mouseX, mouseY)
            .also { super<UpgradesWidget>.preRender(stack, mouseX, mouseY) }
    }


    /**
     * This is called by the basecontainer screen
     */
    override fun postRender(stack: PoseStack, mouseX: Double, mouseY: Double) =
        super<ConfigWidget>.postRender(stack, mouseX, mouseY)
            .also { super<UpgradesWidget>.postRender(stack, mouseX, mouseY) }

    override var y: Float = 0f
}
