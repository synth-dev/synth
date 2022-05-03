package com.github.sieves.api

import com.github.sieves.api.gui.*
import com.github.sieves.dsl.*
import com.mojang.blaze3d.systems.*
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.*
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.texture.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraftforge.fluids.*
import org.lwjgl.glfw.*


abstract class ApiScreen<T : ApiContainer<E, T>, E : ApiTile<E>>(
    container: T,
    playerInv: Inventory
) :
    AbstractContainerScreen<T>(container, playerInv, container.tile.name), BaseWidget<E> {
    protected abstract val texture: ResourceLocation
    private val widgets = "textures/gui/widgets.png".res
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

    open fun drawFluid(
        poseStack: PoseStack,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fluidStack: FluidStack,
        capacityMb: Int
    ) {
        val fluid = fluidStack.fluid ?: return
        val fluidStillSprite: TextureAtlasSprite = getStillFluidSprite(fluidStack)
        val attributes = fluid.attributes
        val fluidColor = attributes.getColor(fluidStack)
        val amount = fluidStack.amount
        var scaledAmount: Int = amount * height / capacityMb
        if (amount > 0 && scaledAmount < 1) {
            scaledAmount = 10
        }
        if (scaledAmount > height) {
            scaledAmount = height
        }
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS)
        val red: Int = fluidColor and 0xff
        val green: Int = fluidColor and 0xff00 shr 8
        val blue: Int = fluidColor and 0xff0000 shr 16
        val alpha: Int = fluidColor and -0x1000000 ushr 24
        RenderSystem.setShaderColor(blue / 255f, green / 255f, red / 255f, 0.9f)
        blit(poseStack, x, y, 80, width, scaledAmount, fluidStillSprite)
    }


    fun getStillFluidSprite(fluidStack: FluidStack): TextureAtlasSprite {
        val minecraft = Minecraft.getInstance()
        val fluid = fluidStack.fluid
        val attributes = fluid.attributes
        val fluidStill = attributes.getStillTexture(fluidStack)
        return minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidStill)
    }


    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        renderBackground(pPoseStack)
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
        renderToolTips(pPoseStack, pMouseX.toDouble(), pMouseY.toDouble())
        renderTooltip(pPoseStack, pMouseX, pMouseY)
        postRender(pPoseStack, pMouseX.toDouble(), pMouseY.toDouble())
    }

    protected open fun renderMain(stack: PoseStack, mouseX: Double, mouseY: Double) = Unit
    protected open fun renderWidgets(stack: PoseStack, mouseX: Double, mouseY: Double) = Unit
    protected open fun renderOverlayWidgets(stack: PoseStack, mouseX: Double, mouseY: Double) = Unit
    protected open fun renderToolTips(stack: PoseStack, mouseX: Double, mouseY: Double) = Unit

}

inline fun <reified R, reified E : ApiTile<E>> R.configure(
    stack: PoseStack,
    mouseX: Double,
    mouseY: Double
): R where R : ApiScreen<E, R>, R : ConfigWidget<R> {
    this.preRender(stack, mouseX, mouseY)
    return this
}
