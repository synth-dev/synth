package com.github.sieves.content.machines.core

import com.github.sieves.api.ApiScreen
import com.github.sieves.api.gui.ConfigWidget
import com.github.sieves.api.ApiConfig
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.energy.CapabilityEnergy
import java.text.NumberFormat

@Suppress("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES")
class CoreScreen(
    val container: CoreContainer, playerInv: Inventory
) : ApiScreen<CoreContainer, CoreTile>(container, playerInv), ConfigWidget<CoreTile> {
    override val texture: ResourceLocation = "textures/gui/farmer_gui.png".resLoc
    override fun renderMain(stack: PoseStack, mouseX: Double, mouseY: Double) {
        blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight)
    }

    override fun renderOverlayWidgets(stack: PoseStack, mouseX: Double, mouseY: Double) {
        renderCharger(stack, mouseX, mouseY)
        val power = (tile().energy.energyStored / tile().energy.maxEnergyStored.toFloat()) * 68
        blit(stack, this.leftPos + 165, this.topPos + 8, 0, 114, 3, power.toInt())
    }

    override fun renderToolTips(stack: PoseStack, mouseX: Double, mouseY: Double) {
        if (isHovered(163, 8, 5, 67, mouseX, mouseY)) {
            if (hasShiftDown())
                drawTooltip(
                    stack, "using: §6${
                        NumberFormat.getIntegerInstance().format(tile().powerCost)
                    }FE/t", mouseX, mouseY
                )
            else
                drawTooltip(
                    stack, "power: §6${
                        NumberFormat.getIntegerInstance().format(tile().energy.energyStored)
                    }FE/${NumberFormat.getIntegerInstance().format(tile().energy.maxEnergyStored)}", mouseX, mouseY
                )
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
    override val config: ApiConfig get() = container.tile.getConfig()
    override var configWidth: Float = 0f
    override var configure: Boolean = false

    /**
     * Pass a reference to our tile
     */
    override val tile: () -> CoreTile
        get() = { container.tile }

    /**
     * This is called by the basecontainer screen
     */
    override fun preRender(stack: PoseStack, mouseX: Double, mouseY: Double) {
        super<ConfigWidget>.preRender(stack, mouseX, mouseY)
    }


    /**
     * This is called by the basecontainer screen
     */
    override fun postRender(stack: PoseStack, mouseX: Double, mouseY: Double) =
        super<ConfigWidget>.postRender(stack, mouseX, mouseY)

}
