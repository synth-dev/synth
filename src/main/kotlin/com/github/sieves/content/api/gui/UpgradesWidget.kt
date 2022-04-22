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
import org.lwjgl.glfw.GLFW
import java.text.NumberFormat


interface UpgradesWidget<T : ApiTile<T>> : BaseWidget<T> {
    val config: Configuration

    //19 x 22, 237, 167
    var y: Float

    private val yOff: Int get() = y.toInt()

    /**
     * This is called by the basecontainer screen
     */
    override fun preRender(stack: PoseStack, mouseX: Double, mouseY: Double) {
        if (configure) {
            if (y < 87) y += Minecraft.getInstance().deltaFrameTime * 12
            else if (y >= 87) y = 87f
        } else {
            if (y > 37) y -= Minecraft.getInstance().deltaFrameTime * 12
            else if (y <= 37) y = 37f
        }
        if (isHovered(-17, yOff, 19, 19, mouseX, mouseY))
            RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1f)
        blit(stack, this.leftPos() - 19, this.topPos() + yOff, 237, 168, 19, 21)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        if (isHovered(-17, yOff + 24, 19, 19, mouseX, mouseY))
            RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1f)
        blit(stack, this.leftPos() - 19, this.topPos() + yOff + 23, 237, 168, 19, 21)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

    }


    /**
     * This is called by the basecontainer screen
     */
    override fun postRender(stack: PoseStack, mouseX: Double, mouseY: Double) {
        var angle = (System.currentTimeMillis() / 10 % 360).toFloat()
        var rotation: Quaternion = Vector3f.YP.rotationDegrees(180f)
        val speed = tile().getConfig().upgrades.getStackInSlot(0)
        val speedCount = speed.count
        val speedOffset = if (speedCount >= 10) 28 else 22
        val efficiency = tile().getConfig().upgrades.getStackInSlot(1)
        val efficiencyCount = efficiency.count
        val efficiencyOffset = if (efficiencyCount >= 10) 28 else 22
        if (isClicked(-17, yOff, 19, 19, mouseX, mouseY)) {
            Registry.Net.sendToServer(Registry.Net.TakeUpgrade {
                this.blockPos = tile().blockPos
                this.count = if (isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) 64 else 1
                this.slot = 0
            })
        }
        if (isHovered(-17, yOff, 19, 19, mouseX, mouseY)) {
            rotation = Vector3f.YP.rotationDegrees(angle)
            rotation.mul(Vector3f.XN.rotationDegrees(angle))
            drawTooltip(
                stack,
                "speed: §6${
                    NumberFormat.getInstance().format(tile().getConfig().speedModifier)
                }X",
                mouseX,
                mouseY
            )
            drawString(
                stack,
                speedCount.toString(),
                this.leftPos() - speedOffset + (1.4f),
                this.topPos() + yOff + 15f,
                0x404040
            )
            drawString(
                stack,
                speedCount.toString(),
                this.leftPos() - speedOffset + 1f,
                this.topPos() + yOff + 13.8f,
                0xFFFFFF
            )
        }
        renderItem(-8f, yOff + 10f, 2f, rotation, speed)

        angle = (((System.currentTimeMillis()) + 1000) / 10 % 360).toFloat()
        rotation = Vector3f.YP.rotationDegrees(180f)
        if (isClicked(-17, yOff + 24, 19, 19, mouseX, mouseY)) {
            Registry.Net.sendToServer(Registry.Net.TakeUpgrade {
                this.blockPos = tile().blockPos
                this.count = if (isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) 64 else 1
                this.slot = 1
            })
        }
        if (isHovered(-17, yOff + 24, 19, 19, mouseX, mouseY)) {
            rotation = Vector3f.YP.rotationDegrees(angle)
            rotation.mul(Vector3f.XN.rotationDegrees(angle))
            drawTooltip(
                stack,
                "efficiency: §6${
                    NumberFormat.getInstance().format(tile().getConfig().efficiencyModifier)
                }X",
                mouseX,
                mouseY
            )

            drawString(
                stack,
                efficiencyCount.toString(),
                this.leftPos() - efficiencyOffset + (1.5f),
                this.topPos() + yOff + 38f,
                0x404040
            )
            drawString(
                stack,
                efficiencyCount.toString(),
                this.leftPos() - efficiencyOffset + 1f,
                this.topPos() + yOff + 38f,
                0xFFFFFF
            )
        }

        renderItem(-8f, yOff + 33f, 2f, rotation, efficiency)

    }

}