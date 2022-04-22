@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.github.sieves.content.api.gui

import com.github.sieves.content.api.ApiTile
import com.github.sieves.content.tile.internal.Configuration
import com.github.sieves.registry.Registry
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.*
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

interface ConfigWidget<T : ApiTile<T>> : BaseWidget<T> {
    val config: Configuration
    var configWidth: Float

    /**
     * This is called by the basecontainer screen
     */
    override fun preRender(stack: PoseStack, mouseX: Double, mouseY: Double) {
        if (configWidth > 0f) configWidth -= Minecraft.getInstance().deltaFrameTime * 20
        if (configWidth <= 0) configWidth = 0f
        if (!configure) {
            if (isHovered(-22, 8, 22, 27, mouseX, mouseY)) RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1.0f)
            blit(stack, this.leftPos() - 22, this.topPos() + 8, 233, 99, 22, 27)
            if (isClicked(-22, 8, 22, 27, mouseX, mouseY)) {
                configure = !configure
                playClickSound()
            }
            configWidth = 58f
            RenderSystem.setShaderColor(1f, 1f, 1f, 1.0f)
        } else {
            stack.pushPose()
            stack.translate(configWidth.toDouble(), 0.0, 0.0)
            blit(stack, this.leftPos() - 58, this.topPos() + 8, 197, 0, 58, 71)
            renderSideButtons(stack, mouseX, mouseY)
            renderAutoButtons(stack, mouseX, mouseY)
            stack.popPose()
            if (isClicked(-14, 16, 10, 12, mouseX, mouseY)) {
                configure = !configure
                playClickSound()
            }

            if (isHovered(-17, 14, 19, 19, mouseX, mouseY))
                RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1f)
            blit(stack, this.leftPos() - 19, this.topPos() + 13, 237, 168, 19, 21)

            if (isHovered(-17, 14, 19, 19, mouseX, mouseY))
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }
    }

    /**
     * Renders the buttons on the config window overlays
     */
    private fun renderSideButtons(stack: PoseStack, mouseX: Double, mouseY: Double) {
        renderSideButton(stack, mouseX, mouseY, Configuration.Side.Front, 24, 39)
        renderSideButton(stack, mouseX, mouseY, Configuration.Side.Left, 12, 39)
        renderSideButton(stack, mouseX, mouseY, Configuration.Side.Right, 36, 39)
        renderSideButton(stack, mouseX, mouseY, Configuration.Side.Back, 36, 51)
        renderSideButton(stack, mouseX, mouseY, Configuration.Side.Top, 24, 27)
        renderSideButton(stack, mouseX, mouseY, Configuration.Side.Bottom, 24, 51)
    }

    /**
     * Renders the auto import/export buttons
     */
    private fun renderAutoButtons(stack: PoseStack, mouseX: Double, mouseY: Double) {
        if (isClicked(-60 + 9, 17, 13, 13, mouseX, mouseY)) {
            playClickSound()
            config.autoImport = !config.autoImport
            Registry.Net.sendToServer(Registry.Net.Configure {
                config = this@ConfigWidget.config
                blockPos = tile().blockPos
                world = tile().level!!.dimension()
            })
        }

        if (isClicked(-61 + 27, 17, 13, 13, mouseX, mouseY)) {
            playClickSound()
            config.autoExport = !config.autoExport
            Registry.Net.sendToServer(Registry.Net.Configure {
                config = this@ConfigWidget.config
                blockPos = tile().blockPos
                world = tile().level!!.dimension()
            })
        }
        if (config.autoImport)
            blit(stack, this.leftPos() + -60 + 9 + 1, this.topPos() + 17, 14, 243, 13, 13)
        blit(stack, this.leftPos() + -60 + 9, this.topPos() + 17, 0, 242, 14, 14)

        if (config.autoExport)
            blit(stack, this.leftPos() + -58 + 27 - 2, this.topPos() + 17 , 14, 243, 13, 13)
        blit(stack, this.leftPos() + -61 + 27, this.topPos() + 17, 0, 242, 14, 14)
    }

    /**
     * Renders the given side button correctly
     */
    private fun renderSideButton(
        stack: PoseStack, mouseX: Double, mouseY: Double, side: Configuration.Side, x: Int, y: Int
    ) {
        val direction = tile().getRelative(side)
        val current = config[direction]
        val dX = -58 + x
        val dy = 8 + y

        RenderSystem.setShaderColor(current.color.x(), current.color.y(), current.color.z(), 1.0f)
        if (current != Configuration.SideConfig.None)
            blit(stack, this.leftPos() + dX + 1, this.topPos() + dy + 1, 225, 100, 9, 9)

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        if (!isHovered(dX, dy, 9, 9, mouseX, mouseY)) {
            blit(stack, this.leftPos() + dX, this.topPos() + dy, 205, 100, 10, 10)
        } else {
            blit(stack, this.leftPos() + dX, this.topPos() + dy, 215, 100, 10, 10)
        }

        if (isClicked(dX, dy, 9, 9, mouseX, mouseY)) {
            if (isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) config[direction] = Configuration.SideConfig.None
            else config[direction] = current.next
            playClickSound()

            Registry.Net.sendToServer(Registry.Net.Configure {
                config = this@ConfigWidget.config
                blockPos = this@ConfigWidget.tile().blockPos
                world = this@ConfigWidget.tile().level!!.dimension()
            })
        }

    }

    /**
     * This is used to render our tooltip
     */
    private fun renderButtonToolTip(
        stack: PoseStack, mouseX: Double, mouseY: Double, side: Configuration.Side, x: Int, y: Int
    ) {
        val direction = tile().getRelative(side)
        val dX = -58 + x
        val dy = 8 + y
        if (isHovered(dX, dy, 9, 9, mouseX, mouseY)) {
            if (!hasShiftDown()) drawTooltip(
                stack, "[${side.name}]: §6${config[direction].displayName}", mouseX, mouseY
            )
            else drawTooltip(
                stack,
                "[${direction.name.lowercase().capitalize()}]: §6${config[direction].displayName}",
                mouseX,
                mouseY
            )
        }
    }

    /**
     * This is called by the basecontainer screen
     */
    override fun postRender(stack: PoseStack, mouseX: Double, mouseY: Double) {
        val rotation: Quaternion = Vector3f.YP.rotationDegrees(180f)
        val angle = (((System.currentTimeMillis()) + 2000) / 10 % 360).toFloat()
        rotation.mul(Vector3f.YP.rotationDegrees(angle))
        if (!configure) {
            if (isClicked(-22, 8, 22, 27, mouseX, mouseY)) {
                drawTooltip(
                    stack, "configure: §6${tile().name.string} ", mouseX, mouseY
                )
            }
            renderItem(-9f, 21f, 3.0f, rotation, ItemStack(tile().blockState.block))
            return
        }
        renderButtonToolTip(
            stack, mouseX, mouseY, Configuration.Side.Front, 24, 39
        )
        renderButtonToolTip(
            stack, mouseX, mouseY, Configuration.Side.Left, 12, 39
        )
        renderButtonToolTip(
            stack, mouseX, mouseY, Configuration.Side.Right, 36, 39
        )
        renderButtonToolTip(
            stack, mouseX, mouseY, Configuration.Side.Back, 36, 51
        )
        renderButtonToolTip(
            stack, mouseX, mouseY, Configuration.Side.Top, 24, 27
        )
        renderButtonToolTip(
            stack, mouseX, mouseY, Configuration.Side.Bottom, 24, 51
        )
        if (isHovered(-58 + 9 - 2, 16, 13, 13, mouseX, mouseY)) {
            drawTooltip(
                stack, "auto import: §6${if (config.autoImport) "enabled" else "disabled"} ", mouseX, mouseY
            )
        }
        if (isHovered(-58 + 27 - 2, 16, 13, 13, mouseX, mouseY)) {
            drawTooltip(
                stack, "auto export: §6${if (config.autoExport) "enabled" else "disabled"} ", mouseX, mouseY
            )
        }

        renderItem(-8f, 23f, 2.0f, rotation, ItemStack(tile().blockState.block))
    }
}