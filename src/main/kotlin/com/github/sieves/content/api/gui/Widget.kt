package com.github.sieves.content.api.gui

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

interface Widget {

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
}