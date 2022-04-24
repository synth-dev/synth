package com.github.sieves.api.gui

import com.github.sieves.api.ApiTile
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW

interface BaseWidget<T : ApiTile<T>> {

    /**
     * This is used to "pass the blit" to implementations
     */
    val blit: (stack: PoseStack, x: Int, y: Int, uOffset: Int, vOffset: Int, uWidth: Int, vHeight: Int) -> Unit

    /**
     * This is used to pass the draw string context
     */
    val drawString: (stack: PoseStack, text: String, y: Float, z: Float, color: Int) -> Unit

    /**
     * This is used to pass the draw tooltip context
     */
    val drawTooltip: (stack: PoseStack, text: String, y: Double, z: Double) -> Unit

    /**
     * Passes the isHovered method
     */
    val isHovered: (x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double) -> Boolean

    /**
     * Passes the isHovered method
     */
    val isClicked: (x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double) -> Boolean

    /**
     * Passes the isHovered method
     */
    val isRightClicked: (x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double) -> Boolean


    /**
     * Pass a reference to our tile
     */
    val tile: () -> T

    /**
     * Pass the left position
     */
    val leftPos: () -> Int

    /**
     * Pass the top position
     */
    val topPos: () -> Int

    /**
     * Pass the image position
     */
    val imageWidth: () -> Int

    /**
     * Pass the height position
     */
    val imageHeight: () -> Int

    /**
     * Used to show if the configuration is open or not
     */
    var configure: Boolean


    /**
     * This is called by the basecontainer screen
     */
    fun preRender(pPoseStack: PoseStack, mouseX: Double, mouseY: Double) = Unit

    /**
     * This is called by the basecontainer screen
     */
    fun postRender(pPoseStack: PoseStack, mouseX: Double, mouseY: Double) = Unit

    /**
     * Uses glfw to see if the key is down
     */
    fun isKeyDown(key: Int) = GLFW.glfwGetKey(Minecraft.getInstance().window.window, key) == GLFW.GLFW_PRESS

    /**
     * Plays the click sound for the local player
     */
    fun playClickSound() {
        val pos = Minecraft.getInstance().player?.position() ?: Vec3.ZERO
        Minecraft.getInstance().level?.playSound(
            Minecraft.getInstance().player,
            pos.x,
            pos.y,
            pos.z,
            SoundEvents.UI_BUTTON_CLICK,
            SoundSource.NEUTRAL,
            0.25f,
            1f
        )
    }

    /**
     * Renders an item on the gui at the given position on top of everything
     */
    fun renderItem(x: Float, y: Float, scale: Float, rotation: Quaternion, item: ItemStack) {
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        val poseStack = RenderSystem.getModelViewStack()
        poseStack.pushPose()

        val top = topPos()
        val left = leftPos()
        poseStack.translate(left.toDouble(), top.toDouble(), 100.0f.toDouble())
        poseStack.translate(x.toDouble(), (y.toDouble()), 0.0)
        poseStack.scale(1.0f, -1.0f, 1.0f)
        poseStack.scale(scale, scale, scale)
        RenderSystem.applyModelViewMatrix()
        val blockPoseStack = PoseStack()
        blockPoseStack.pushPose()
        blockPoseStack.mulPose(rotation)
        blockPoseStack.scale(8f, 8f, 8f)
        val bufferSource = Minecraft.getInstance().renderBuffers().bufferSource()
        Minecraft.getInstance().itemRenderer.renderStatic(
            item,
            ItemTransforms.TransformType.FIXED,
            15728880,
            OverlayTexture.NO_OVERLAY,
            blockPoseStack,
            bufferSource,
            0
        )
        bufferSource.endBatch()
        poseStack.popPose()
        RenderSystem.applyModelViewMatrix()
    }
}