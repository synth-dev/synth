package com.github.sieves.content.modules.io

import com.github.sieves.dsl.*
import com.mojang.blaze3d.systems.*
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.*

class FilterScreen(container: FilterContainer, inventory: Inventory) :
    AbstractContainerScreen<FilterContainer>(container, inventory, TextComponent("Filter")) {

    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pPoseStack)
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
        this.renderTooltip(pPoseStack, pMouseX, pMouseY)
    }
    override fun renderBg(stack: PoseStack, pPartialTick: Float, pMouseX: Int, pMouseY: Int) {
        RenderSystem.setShaderTexture(0, Texture)
        blit(stack, guiLeft, guiTop, 0, 0, 175, 143)
    }

    override fun renderLabels(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int) {
        font.draw(pPoseStack, title, titleLabelX.toFloat(), titleLabelY.toFloat() - 2, 4210752)
        font.draw(pPoseStack, playerInventoryTitle, inventoryLabelX.toFloat(), inventoryLabelY.toFloat() - 20, 4210752)
    }

    companion object {
        private val Texture = "textures/gui/filter_gui.png".res
    }

}