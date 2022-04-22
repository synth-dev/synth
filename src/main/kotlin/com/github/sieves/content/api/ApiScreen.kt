package com.github.sieves.content.api

import com.github.sieves.content.api.gui.BaseWidget
import com.github.sieves.content.api.gui.ConfigWidget
import com.github.sieves.content.api.gui.UpgradesWidget
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.TextComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.glfw.GLFW


abstract class ApiScreen<T : ApiContainer<E, T>, E : ApiTile<E>>(
    container: T,
    playerInv: Inventory
) :
    AbstractContainerScreen<T>(container, playerInv, container.tile.name), BaseWidget<E> {
    protected abstract val texture: ResourceLocation
    private val widgets = "textures/gui/widgets.png".resLoc
    private var lastClick = System.currentTimeMillis()

    /**
     * This is used to "pass the blit" to implementations
     */
    override val blit: (stack: PoseStack, x: Int, y: Int, uOffset: Int, vOffset: Int, uWidth: Int, vHeight: Int) -> Unit =
        ::blit

    /**
     * Passes the isHovered method
     */
    override val isRightClicked: (x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double) -> Boolean =
        ::isRightClicked

    /**
     * Pass the left position
     */
    override val leftPos: () -> Int
        get() = { this.leftPos }

    /**
     * Pass the top position
     */
    override val topPos: () -> Int
        get() = { this.topPos }

    /**
     * This is used to pass the draw tooltip context
     */
    override val drawTooltip: (stack: PoseStack, text: String, x: Double, y: Double) -> Unit
        get() = { stack, text, x, y -> this.renderTooltip(stack, TextComponent(text), x.toInt(), y.toInt()) }

    /**
     * Pass the image position
     */
    override val imageWidth: () -> Int
        get() = { this.imageWidth }

    /**
     * Pass the height position
     */
    override val imageHeight: () -> Int
        get() = { this.imageHeight }

    /**
     * This is used to pass the draw string context
     */
    override val drawString: (stack: PoseStack, text: String, y: Float, z: Float, color: Int) -> Unit
        get() = { stack, text, y, z, color ->
            font.draw(stack, TextComponent(text), y, z, color)
        }

    /**
     * Passes the isHovered method
     */
    override val isHovered: (x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double) -> Boolean =
        ::isHovering

    /**
     * Passes the isHovered method
     */
    override val isClicked: (x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double) -> Boolean =
        ::isClicked

    init {
        leftPos = 0
        topPos = 0
        imageWidth = 175
        imageHeight = 166
    }

    override fun renderBg(pPoseStack: PoseStack, pPartialTick: Float, pMouseX: Int, pMouseY: Int) {
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, widgets)
        preRender(pPoseStack, pMouseX.toDouble(), pMouseY.toDouble())
        renderWidgets(pPoseStack, pMouseX.toDouble(), pMouseY.toDouble())
        RenderSystem.setShaderTexture(0, texture)
        renderMain(pPoseStack, pMouseX.toDouble(), pMouseY.toDouble())
        RenderSystem.setShaderTexture(0, widgets)
        renderOverlayWidgets(pPoseStack, pMouseX.toDouble(), pMouseY.toDouble())
    }

    protected fun isClicked(x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double): Boolean {
        val now = System.currentTimeMillis()
        if (isHovering(x, y, width, height, mouseX, mouseY) && GLFW.glfwGetMouseButton(
                Minecraft.getInstance().window.window,
                GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == GLFW.GLFW_PRESS && now - lastClick > 250
        ) {
            lastClick = now
            return true
        }
        return false
    }

    protected fun isRightClicked(x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double): Boolean {
        val now = System.currentTimeMillis()
        if (isHovering(x, y, width, height, mouseX, mouseY) && GLFW.glfwGetMouseButton(
                Minecraft.getInstance().window.window,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT
            ) == GLFW.GLFW_PRESS && now - lastClick > 250
        ) {
            lastClick = now
            return true
        }
        return false
    }

    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
        renderToolTips(pPoseStack, pMouseX.toDouble(), pMouseY.toDouble())
        postRender(pPoseStack, pMouseX.toDouble(), pMouseY.toDouble())
    }


    protected open fun renderMain(stack: PoseStack, mouseX: Double, mouseY: Double) = Unit
    protected open fun renderWidgets(stack: PoseStack, mouseX: Double, mouseY: Double) = Unit
    protected open fun renderOverlayWidgets(stack: PoseStack, mouseX: Double, mouseY: Double) = Unit
    protected open fun renderToolTips(stack: PoseStack, mouseX: Double, mouseY: Double) = Unit

}

inline fun <reified R, reified E : ApiTile<E>> R.configPre(
    stack: PoseStack,
    mouseX: Double,
    mouseY: Double
): R where R : ApiScreen<E, R>, R : ConfigWidget<R> {
    this.preRender(stack, mouseX, mouseY)
    return this
}

inline fun <reified R, reified E : ApiTile<E>> R.upgradePre(
    stack: PoseStack,
    mouseX: Double,
    mouseY: Double
): R where R : ApiScreen<E, R>, R : UpgradesWidget<R> {
    this.preRender(stack, mouseX, mouseY)
    return this
}


inline fun <reified R, reified E : ApiTile<E>> R.configPost(
    stack: PoseStack,
    mouseX: Double,
    mouseY: Double
): R where R : ApiScreen<E, R>, R : ConfigWidget<R> {
    this.postRender(stack, mouseX, mouseY)
    return this
}

inline fun <reified R, reified E : ApiTile<E>> R.upgradePost(
    stack: PoseStack,
    mouseX: Double,
    mouseY: Double
): R where R : ApiScreen<E, R>, R : UpgradesWidget<R> {
    this.postRender(stack, mouseX, mouseY)
    return this
}