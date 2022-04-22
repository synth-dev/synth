//package com.github.sieves.content.sieve
//
//import com.github.sieves.content.tile.internal.Configuration
//import com.github.sieves.registry.Registry.Net
//import com.github.sieves.util.resLoc
//import com.mojang.blaze3d.systems.RenderSystem
//import com.mojang.blaze3d.vertex.PoseStack
//import net.minecraft.client.Minecraft
//import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
//import net.minecraft.client.renderer.GameRenderer
//import net.minecraft.core.Direction
//import net.minecraft.network.chat.Component
//import net.minecraft.network.chat.TextComponent
//import net.minecraft.sounds.SoundEvents
//import net.minecraft.sounds.SoundSource
//import net.minecraft.world.entity.player.Inventory
//import net.minecraft.world.phys.Vec3
//import org.lwjgl.glfw.GLFW
//import java.text.NumberFormat
//import kotlin.math.roundToInt
//
//class SieveScreen(
//    val container: SieveContainer, playerInv: Inventory, component: Component
//) : AbstractContainerScreen<SieveContainer>(container, playerInv, component) {
//    private var configure = false
//    private var lastTime = System.currentTimeMillis()
//    private var scaleWidth = 0f
//    private var scaleHeight = 0f
//    private val tile: SieveTile? get() = container.tile
//    private val config: Configuration? get() = tile?.config
//
//    init {
//        leftPos = 0
//        topPos = 0
//        imageWidth = 175
//        imageHeight = 166
//    }
//
//
//    override fun renderBg(stack: PoseStack, partialTicks: Float, mouseX: Int, mouseY: Int) {
//        RenderSystem.setShader(GameRenderer::getPositionColorTexShader)
//
//        RenderSystem.setShaderTexture(0, Widgets)
//        renderConfigure(stack, mouseX.toDouble(), mouseY.toDouble())
//
//        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
//        RenderSystem.setShaderTexture(0, Texture)
//        blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight)
//        RenderSystem.setShaderTexture(0, Widgets)
//
//        val progress = container.data.get(0)
//        val percent = ((progress / 100f) * 20f).roundToInt()
//        blit(stack, this.leftPos + 90, this.topPos + 35, 0, 99, percent, 15)
//        val energy = container.data.get(1)
//        val energyPercent = ((energy / 100f) * 68).roundToInt()
//        blit(stack, this.leftPos + 165, this.topPos + 8, 0, 114, 3, energyPercent)
//
//        if (configure) {
//            renderButtonToolTip(
//                stack,
//                mouseX.toDouble(),
//                mouseY.toDouble(),
//                Configuration.Side.Front,
//                Direction.NORTH,
//                24,
//                39
//            )
//            renderButtonToolTip(
//                stack,
//                mouseX.toDouble(),
//                mouseY.toDouble(),
//                Configuration.Side.Left,
//                Direction.WEST,
//                12,
//                39
//            )
//            renderButtonToolTip(
//                stack,
//                mouseX.toDouble(),
//                mouseY.toDouble(),
//                Configuration.Side.Right,
//                Direction.EAST,
//                36,
//                39
//            )
//            renderButtonToolTip(
//                stack,
//                mouseX.toDouble(),
//                mouseY.toDouble(),
//                Configuration.Side.Back,
//                Direction.SOUTH,
//                36,
//                51
//            )
//            renderButtonToolTip(
//                stack,
//                mouseX.toDouble(),
//                mouseY.toDouble(),
//                Configuration.Side.Top,
//                Direction.UP,
//                24,
//                27
//            )
//            renderButtonToolTip(
//                stack,
//                mouseX.toDouble(),
//                mouseY.toDouble(),
//                Configuration.Side.Bottom,
//                Direction.DOWN,
//                24,
//                51
//            )
//            renderAutoButtonsHover(stack, mouseX.toDouble(), mouseY.toDouble())
//        }
//
//    }
//
//    private fun renderAutoButtonsHover(stack: PoseStack, mouseX: Double, mouseY: Double) {
//        if (config == null) return
//        if (isHovering(-58 + 9, 16, 13, 13, mouseX, mouseY)) {
//            renderTooltip(
//                stack,
//                TextComponent("Auto import: §6${if (config!!.autoImport) "enabled" else "disabled"} "),
//                mouseX.toInt(),
//                mouseY.toInt()
//            )
//        }
//        if (isHovering(-58 + 27, 16, 13, 13, mouseX, mouseY)) {
//            renderTooltip(
//                stack,
//                TextComponent("Auto export: §6${if (config!!.autoExport) "enabled" else "disabled"} "),
//                mouseX.toInt(),
//                mouseY.toInt()
//            )
//        }
//    }
//
//    private fun renderAutoButtons(stack: PoseStack, mouseX: Double, mouseY: Double) {
//        if (config == null) return
//        var hoveringFirst = false
//        var hoveringSecond = false
//
//        if (isHovering(-58 + 9, 16, 13, 13, mouseX, mouseY)) {
//            val now = System.currentTimeMillis()
//            if (GLFW.glfwGetMouseButton(
//                    Minecraft.getInstance().window.window,
//                    GLFW.GLFW_MOUSE_BUTTON_LEFT
//                ) == GLFW.GLFW_PRESS && now - lastTime > 300
//            ) {
//                val pos = Minecraft.getInstance().player?.position() ?: Vec3.ZERO
//                Minecraft.getInstance().level?.playSound(
//                    Minecraft.getInstance().player,
//                    pos.x,
//                    pos.y,
//                    pos.z,
//                    SoundEvents.UI_BUTTON_CLICK,
//                    SoundSource.NEUTRAL,
//                    1f,
//                    1f
//                )
//                config!!.autoImport = !config!!.autoImport
//                lastTime = now
//                Net.sendToServer(Net.Configure {
//                    config = this@SieveScreen.config!!
//                    blockPos = tile!!.blockPos
//                    world = tile!!.level!!.dimension()
//                })
//            }
//        }
//
//        if (isHovering(-58 + 27, 16, 13, 13, mouseX, mouseY)) {
//            val now = System.currentTimeMillis()
//            if (GLFW.glfwGetMouseButton(
//                    Minecraft.getInstance().window.window,
//                    GLFW.GLFW_MOUSE_BUTTON_LEFT
//                ) == GLFW.GLFW_PRESS && now - lastTime > 300
//            ) {
//                val pos = Minecraft.getInstance().player?.position() ?: Vec3.ZERO
//                Minecraft.getInstance().level?.playSound(
//                    Minecraft.getInstance().player,
//                    pos.x,
//                    pos.y,
//                    pos.z,
//                    SoundEvents.UI_BUTTON_CLICK,
//                    SoundSource.NEUTRAL,
//                    1f,
//                    1f
//                )
//                config!!.autoExport = !config!!.autoExport
//                lastTime = now
//                Net.sendToServer(Net.Configure {
//                    config = this@SieveScreen.config!!
//                    blockPos = tile!!.blockPos
//                    world = tile!!.level!!.dimension()
//                })
//            }
//        }
//        if (config!!.autoImport)
//            blit(stack, this.leftPos + -58 + 9, this.topPos + 16, 0, 242, 14, 14)
//        if (config!!.autoExport)
//            blit(stack, this.leftPos + -58 + 27, this.topPos + 16, 0, 242, 14, 14)
//    }
//
//    private fun renderConfigure(stack: PoseStack, mouseX: Double, mouseY: Double) {
//        val y = 8
//        if ((isHovering(-22, y, 22, 27, mouseX, mouseY) && !configure) || (isHovering(
//                -22 + 8,
//                y + 8,
//                10,
//                12,
//                mouseX,
//                mouseY
//            ) && configure)
//        ) {
//            if (!configure)
//                RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1.0f)
//            val now = System.currentTimeMillis()
//            if (GLFW.glfwGetMouseButton(
//                    Minecraft.getInstance().window.window,
//                    GLFW.GLFW_MOUSE_BUTTON_LEFT
//                ) == GLFW.GLFW_PRESS && now - lastTime > 300
//            ) {
//                val pos = Minecraft.getInstance().player?.position() ?: Vec3.ZERO
//                Minecraft.getInstance().level?.playSound(
//                    Minecraft.getInstance().player,
//                    pos.x,
//                    pos.y,
//                    pos.z,
//                    SoundEvents.UI_BUTTON_CLICK,
//                    SoundSource.NEUTRAL,
//                    1f,
//                    1f
//                )
//                configure = !configure
//                lastTime = now
//            }
//        }
//        if (!configure) {
//            blit(stack, this.leftPos - 22, this.topPos + y, 0, 72, 22, 27)
//            scaleWidth = 58f
//        } else {
//            stack.pushPose()
//            if (scaleWidth > 0f) {
//                scaleWidth -= Minecraft.getInstance().deltaFrameTime * 20
//            }
//            if (scaleWidth <= 0) {
//                scaleWidth = 0f
//            }
//
//            stack.translate(scaleWidth.toDouble(), 0.0, 0.0)
//            blit(stack, this.leftPos - 58, this.topPos + y, 0, 0, 58, 71)
//
//            renderButton(stack, mouseX, mouseY, Configuration.Side.Front, Direction.NORTH, 24, 39)
//            renderButton(stack, mouseX, mouseY, Configuration.Side.Left, Direction.WEST, 12, 39)
//            renderButton(stack, mouseX, mouseY, Configuration.Side.Right, Direction.EAST, 36, 39)
//            renderButton(stack, mouseX, mouseY, Configuration.Side.Back, Direction.SOUTH, 36, 51)
//            renderButton(stack, mouseX, mouseY, Configuration.Side.Top, Direction.UP, 24, 27)
//            renderButton(stack, mouseX, mouseY, Configuration.Side.Bottom, Direction.DOWN, 24, 51)
//            if (configure) renderAutoButtons(stack, mouseX, mouseY)
//
//
//
//            stack.popPose()
//        }
//
//
//    }
//
//    private fun renderButtonToolTip(
//        stack: PoseStack,
//        mouseX: Double,
//        mouseY: Double,
//        side: Configuration.Side,
//        dir: Direction,
//        x: Int,
//        y: Int
//    ) {
//        val direction = tile?.getRelative(side) ?: dir
//
//        if (config == null) return
//        val dX = -58 + x
//        val dy = 8 + y
//        if (isHovering(dX, dy, 9, 9, mouseX, mouseY)) {
//            if (!hasShiftDown())
//                renderTooltip(
//                    stack,
//                    TextComponent(
//                        "[${side.name}]: §6${config!![direction].displayName}"
//                    ),
//                    mouseX.toInt(),
//                    mouseY.toInt()
//                )
//            else
//                renderTooltip(
//                    stack,
//                    TextComponent(
//                        "[${direction.name.lowercase().capitalize()}]: §6${config!![direction].displayName}"
//                    ),
//                    mouseX.toInt(),
//                    mouseY.toInt()
//                )
//        }
//    }
//
//    private fun renderButton(
//        stack: PoseStack,
//        mouseX: Double,
//        mouseY: Double,
//        side: Configuration.Side,
//        dir: Direction,
//        x: Int,
//        y: Int
//    ) {
//        val direction = tile?.getRelative(side) ?: dir
//        if (config == null) return
//        val current = config!![direction]
//        val yOffset = 182 + (current.ordinal * 10)
//        val dX = -58 + x
//        val dy = 8 + y
//        if (current != Configuration.SideConfig.None)
//            blit(stack, this.leftPos + dX, this.topPos + dy, 0, yOffset, 10, 10)
//        if (isHovering(dX, dy, 9, 9, mouseX, mouseY)) {
//            val now = System.currentTimeMillis()
//            if (GLFW.glfwGetMouseButton(
//                    Minecraft.getInstance().window.window,
//                    GLFW.GLFW_MOUSE_BUTTON_LEFT
//                ) == GLFW.GLFW_PRESS && now - lastTime > 200
//            ) {
//                if (GLFW.glfwGetKey(
//                        Minecraft.getInstance().window.window,
//                        GLFW.GLFW_KEY_LEFT_SHIFT
//                    ) == GLFW.GLFW_PRESS
//                ) {
//                    config!![direction] = Configuration.SideConfig.None
//                } else config!![direction] = current.next
//
//                lastTime = now
//                val pos = Minecraft.getInstance().player?.position() ?: Vec3.ZERO
//                Minecraft.getInstance().level?.playSound(
//                    Minecraft.getInstance().player,
//                    pos.x,
//                    pos.y,
//                    pos.z,
//                    SoundEvents.UI_BUTTON_CLICK,
//                    SoundSource.NEUTRAL,
//                    1f,
//                    1f
//                )
//                Net.sendToServer(Net.Configure {
//                    config = this@SieveScreen.config!!
//                    blockPos = tile!!.blockPos
//                    world = tile!!.level!!.dimension()
//                })
//            }
//
//
//        }
//
//    }
//
//    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
//        this.renderBackground(pPoseStack)
//        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
//
//        if (isHovering(
//                90,
//                35,
//                25,
//                15,
//                pMouseX.toDouble(),
//                pMouseY.toDouble()
//            )
//        ) {
//            val progress = container.data.get(0)
//            val time = (tile!!.targetProgress - tile!!.progress) / 20
//            if (!hasShiftDown())
//                renderTooltip(pPoseStack, TextComponent("progress: §6$progress%"), pMouseX, pMouseY)
//            else
//                renderTooltip(pPoseStack, TextComponent("time left: §6${time}s"), pMouseX, pMouseY)
//
//        }
//        if (isHovering(163, 8, 5, 67, pMouseX.toDouble(), pMouseY.toDouble())) {
//            val progress = container.data.get(1)
//            val percent = ((progress / 100f) * 100_000)
//            if (!hasShiftDown())
//                renderTooltip(
//                    pPoseStack,
//                    TextComponent("power: §6${NumberFormat.getIntegerInstance().format(tile?.energyStore?.energyStored)}FE/100,000FE"),
//                    pMouseX,
//                    pMouseY
//                )
//            else
//                renderTooltip(
//                    pPoseStack,
//                    TextComponent("using: §6${tile?.targetEnergy}FE/t"),
//                    pMouseX,
//                    pMouseY)
//        }
//        this.renderTooltip(pPoseStack, pMouseX, pMouseY)
//
////        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
//    }
//
//    companion object {
//        private val Texture = "textures/gui/sieve_gui.png".resLoc
//        private val Widgets = "textures/gui/widgets.png".resLoc
//    }
//}
