package com.github.sieves.content.api.tab

import com.github.sieves.content.api.ApiTab
import com.github.sieves.util.*
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import com.github.sieves.registry.Registry.Net as Net

/**
 * This is used to render our tab
 */
@Suppress("UNCHECKED_CAST")
open class Tab(
    override val key: ResourceLocation, private val spec: TabSpec,
) : ServerTab(key, spec) {


    /**
     * Renders an item on the gui at the given position on top of everything.
     */
    @OnlyIn(Dist.CLIENT)
    fun renderItem(
        x: Float,
        y: Float,
        scale: Float,
        rotation: Quaternion,
        item: ItemStack,
        containerScreen: AbstractContainerScreen<*>
    ) {
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        val poseStack = RenderSystem.getModelViewStack()
        poseStack.pushPose()
        poseStack.translate(0.0, 0.0, 100.0f.toDouble())
        poseStack.translate(x.toDouble(), (y.toDouble()), 100.0)
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


    /**
     * Renders our item
     */
    @OnlyIn(Dist.CLIENT)
    fun preRender(
        stack: PoseStack,
        offsetX: Float,
        offsetY: Float,
        mouseX: Double,
        mouseY: Double,
        container: AbstractContainerScreen<*>
    ): Int = with(container) {
        if (drawMenu) {
            stack.pushPose()
            this@Tab.drawMenu(stack, offsetX, offsetY, mouseX, mouseY, container)
            stack.popPose()

        }

        val yOff = offsetY
        if (isHovered(
                (-19f).toInt(),
                yOff.toInt(),
                19,
                19,
                mouseX,
                mouseY
            ) && spec.drawHover
        ) RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1f)
        else RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.setShaderTexture(0, widgets)
        blit(stack, guiLeft - 19, guiTop + offsetY.toInt(), 237, 168, 19, 21)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        val angle = (System.currentTimeMillis() / 10 % 360).toFloat()
        val rotation = spec.rotationSupplier(this@Tab)
        if (spec.drawSpin) {
            rotation.mul(Vector3f.YP.rotationDegrees(angle))
        }
        if (spec.drawItem) {
            val item = spec.itemstackSupplier(this@Tab)
            renderItem(guiLeft + -8f, guiTop + yOff + 10f, 2f, rotation, item, container)
        }
        RenderSystem.setShaderTexture(0, widgets)
        if (spec.drawToolTip) {
            if (isHovered((-19f).toInt(), yOff.toInt(), 19, 19, mouseX, mouseY) && spec.drawHover) {
                val text = spec.tooltipSupplier(this@Tab)
                renderTooltip(stack, text, mouseX.toInt(), mouseY.toInt())
            }
        }
        if (drawMenu) 72 else if (spec.drawLarger) 27 else 19
    }

    /**
     * Renders our item stuff like tooltiops
     */
    @OnlyIn(Dist.CLIENT)
    fun postRender(
        stack: PoseStack,
        offsetX: Float,
        offsetY: Float,
        mouseX: Double,
        mouseY: Double,
        container: AbstractContainerScreen<*>
    ): Int = with(container) {
        val yOff = offsetY
        if (isRightClicked((-19f).toInt(), yOff.toInt(), 19, 19, mouseX, mouseY)) {
            Net.sendToServer(Net.ClickTab {
                this.key = this@Tab.key
                this.uuid = this@Tab.uuid
            })
        }
        if (isClicked((-19f).toInt(), yOff.toInt(), 19, 19, mouseX, mouseY)) {
            if (spec.hasDrawMenu) drawMenu = !drawMenu
        }
        if (drawMenu) 72 else if (spec.drawLarger) 27 else 19
    }

    /**
     * draws the menu
     */
    @OnlyIn(Dist.CLIENT)
    private fun drawMenu(
        stack: PoseStack,
        offsetX: Float,
        offsetY: Float,
        mouseX: Double,
        mouseY: Double,
        container: AbstractContainerScreen<*>
    ) {
        RenderSystem.setShaderTexture(0, widgets)
        val player = Minecraft.getInstance().player ?: return
        menuData.x = container.guiLeft - offsetX - 39
        menuData.y = container.guiTop + offsetY
        menuData.width = 59f
        menuData.height = 72f
        menuData.poseStack = stack
        container.blit(
            stack,
            menuData.x.toInt(),
            menuData.y.toInt(),
            197,
            0,
            menuData.width.toInt(),
            menuData.height.toInt()
        )

        spec.drawMenu(menuData, player, this, container)
    }

    /**
     * Provide external access to the built propertiers
     */
    override fun getSpec(): TabSpec = spec
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tab) return false

        if (key != other.key) return false
        if (spec != other.spec) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + spec.hashCode()
        return result
    }

    override fun toString(): String {
        return "Tab(key=$key, properties=$spec, nbt=$nbt)"
    }


    companion object {
        private val widgets = "textures/gui/widgets.png".resLoc

        /**
         * This is used delegate registration using properties
         */
        fun register(
            name: ResourceLocation, supplier: () -> TabSpec
        ): ReadOnlyProperty<Any?, Tab> {
            val tab = Tab(name, supplier())
            TabRegistry.registerTab(tab)
            TabRegistry.registerTabFactory(name) {
                tab.cloneWith(it)
            }
            return object : ReadOnlyProperty<Any?, Tab>, Supplier<Tab>, () -> Tab {
                override fun getValue(thisRef: Any?, property: KProperty<*>): Tab = get()
                override fun invoke(): Tab = get()
                override fun get(): Tab = tab
            }
        }
    }


}