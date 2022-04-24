package com.github.sieves.content.io.fluids

import com.github.sieves.api.ApiScreen
import com.github.sieves.api.gui.ConfigWidget
import com.github.sieves.api.gui.UpgradesWidget
import com.github.sieves.api.ApiConfig
import com.github.sieves.util.*
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.energy.CapabilityEnergy
import java.text.NumberFormat

@Suppress("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES")
class FluidsScreen(
    val container: FluidsContainer, playerInv: Inventory
) : ApiScreen<FluidsContainer, FluidsTile>(container, playerInv), ConfigWidget<FluidsTile>, UpgradesWidget<FluidsTile> {
    override val texture: ResourceLocation = "textures/gui/tank_gui.png".resLoc

    override fun renderMain(stack: PoseStack, mouseX: Double, mouseY: Double) {
        blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight)
        blit(stack, guiLeft + 49, guiTop + 10, 0, 167, 70, 60)
        drawFluid(
            stack, guiLeft + 50, guiTop + 10, 70, 60, tile().fluids.getFluidInTank(0), tile().fluids.getTankCapacity(0)
        )

    }

    override fun renderOverlayWidgets(stack: PoseStack, mouseX: Double, mouseY: Double) {
        val power = (tile().energy.energyStored / tile().energy.maxEnergyStored.toFloat()) * 68
        blit(stack, this.leftPos + 165, this.topPos + 8, 0, 114, 3, power.toInt())
//        println(tile().fluids.fluidAmount)

        RenderSystem.setShader(GameRenderer::getPositionColorTexShader)
        RenderSystem.setShaderTexture(0, texture)
        RenderSystem.setShaderColor(0f, 0f, 0f, 1f)
        blit(stack, guiLeft + 49, guiTop + 10, 0, 167, 70, 60)

    }

    override fun renderToolTips(stack: PoseStack, mouseX: Double, mouseY: Double) {
        if (isHovered(83, 56, 9, 11, mouseX, mouseY)) {
            val item = tile().items.getStackInSlot(0)
            if (item.isEmpty) return
            val cap = item.getCapability(CapabilityEnergy.ENERGY)
            if (cap.isPresent) {
                val energy = cap.resolve().get()
                drawTooltip(
                    stack, "power: §6${
                        NumberFormat.getIntegerInstance().format(energy.energyStored)
                    }FE/${NumberFormat.getIntegerInstance().format(energy.maxEnergyStored)}FE", mouseX, mouseY
                )
            }
        }

        if (isHovered(50, 10, 70, 60, mouseX, mouseY)) {
            val fluid = tile().fluids.fluidAmount
            val total = tile().fluids.capacity
            if (!hasShiftDown())
                drawTooltip(
                    stack, "fluid: §6${
                        NumberFormat.getIntegerInstance().format(fluid)
                    }MB/${NumberFormat.getIntegerInstance().format(total)}MB", mouseX, mouseY
                )
            else
                drawTooltip(
                    stack, "fluid: §6${
                        NumberFormat.getIntegerInstance().format(fluid / 1000)
                    }B/${NumberFormat.getIntegerInstance().format(total / 1000)}B", mouseX, mouseY
                )
        }


        if (isHovered(163, 8, 5, 67, mouseX, mouseY)) {
            drawTooltip(
                stack, "power: §6${
                    NumberFormat.getIntegerInstance().format(tile().energy.energyStored)
                }FE/${NumberFormat.getIntegerInstance().format(tile().energy.maxEnergyStored)}", mouseX, mouseY
            )
        }
    }


    //ConfigWidget
    override val config: ApiConfig get() = container.tile.getConfig()
    override var configWidth: Float = 0f
    override var configure: Boolean = false

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

    /**
     * Pass a reference to our tile
     */
    override val tile: () -> FluidsTile
        get() = { container.tile }
    override var y: Float = 0f
}
