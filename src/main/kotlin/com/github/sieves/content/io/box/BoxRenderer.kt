package com.github.sieves.content.io.box

import com.github.sieves.api.ApiRenderer
import com.github.sieves.registry.Registry
import com.github.sieves.util.length
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.phys.Vec3
import java.lang.Float.max

class BoxRenderer : ApiRenderer<BoxTile>() {
    private val texture = ResourceLocation("minecraft", "block/lapis_block")
    private var time = 0f

    override fun render(
        pBlockEntity: BoxTile,
        pPartialTick: Float,
        stack: PoseStack,
        pBufferSource: MultiBufferSource,
        pPackedLight: Int,
        pPackedOverlay: Int
    ) {
        drawBeams(stack, pBufferSource, pBlockEntity)
        drawPower(stack, pBufferSource, pBlockEntity)
        time += Minecraft.getInstance().deltaFrameTime * 0.01f
        time %= 1
    }


    private fun drawBeams(stack: PoseStack, bufferSource: MultiBufferSource, tile: BoxTile) {
        val startX = tile.blockPos.x.toFloat()
        val startY = tile.blockPos.y.toFloat()
        val startZ = tile.blockPos.z.toFloat()
        val start = Vector3f()
        val stop = Vector3f()


        var offsetX = 0f
        var offsetY = 0f
        var offsetZ = 0f
        when (tile.blockState.getValue(DirectionalBlock.FACING)) {
            Direction.DOWN -> {
                offsetY -= 0.1f
            }
            Direction.UP -> {
                offsetY += 0.1f
            }
            Direction.NORTH -> {
                offsetZ -= 0.1f
            }
            Direction.SOUTH -> {
                offsetZ += 0.1f
            }
            Direction.WEST -> {
                offsetX -= 0.1f
            }
            Direction.EAST -> {
                offsetX += 0.1f
            }
        }

        if (tile.getStoredPower() <= 0) return
        val renderLines =
            Minecraft.getInstance().player?.getItemInHand(InteractionHand.MAIN_HAND)?.item == Registry.Items.Linker
        for (link in tile.links.getLinks()) {
            start.set(offsetX, offsetY, offsetZ)
            stop.set(0f, 0f, 0f)
            val stopX = link.key.x - startX
            val stopY = link.key.y - startY
            val stopZ = link.key.z - startZ
            val stop = Vector3f(stopX, stopY, stopZ)

            val length = stop.length(0.01f)

            start.lerp(stop, time)
//            start.set(offsetX, offsetY, offsetZ)
            stop.lerp(stop, time + length)


//            if (time > 0.9) {
//                stop.set(stopX, stopY, stopZ)
//            }
            stack.pushPose()
            stack.translate(0.5, 0.5, 0.5)

            if (renderLines) {

//                val lerper = Vector3f(start.x(), start.y(), start.z())
//                lerper.set(start.x(), start.y(), start.z())

                Minecraft.getInstance().level?.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    tile.blockPos.x + 0.5 + start.x(),
                    tile.blockPos.y + 0.5 + start.y(),
                    tile.blockPos.z + 0.5 + start.z(),
                    0.0,
                    0.0,
                    0.0
                )


//                renderLine(
//                    stack,
//                    buffer,
//                    start.x(),
//                    start.y(),
//                    start.z(),
//                    stop.x(),
//                    stop.y(),
//                    stop.z(),
//                    252 / 255f,
//                    32 / 255f,
//                    3 / 255f
//                )
            }
            stack.popPose()

        }


    }

    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    private fun drawPower(stack: PoseStack, bufferSource: MultiBufferSource, tile: BoxTile) {
        val buffer = bufferSource.getBuffer(RenderType.translucent())
        val sprite = getSprite(texture)

        stack.pushPose()
        stack.translate(0.5, 0.0, 0.5)
        stack.scale(0.4f, 0.4f, 0.4f)

        val scale = max((tile.energy.energyStored / tile.energy.maxEnergyStored.toFloat()), 0.05f)
        when (tile.blockState.getValue(DirectionalBlock.FACING)) {
            Direction.DOWN -> {
                stack.scale(1.0f, scale, 1.0f)
                renderQuad(stack, buffer, -0.3f, 0.3f, 0f, 0.8f, -0.3f, -0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, -0.3f, 0f, 0.8f, 0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, 0.3f, 0f, 0.8f, -0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, -0.3f, -0.3f, 0f, 0.8f, 0.3f, -0.3f, sprite)

            }
            Direction.UP -> {
                stack.mulPose(Quaternion.fromXYZ(Math.toRadians(180.0).toFloat(), 0f, 0f))
                stack.translate(0.0, -2.5, 0.0)
                stack.scale(1.0f, scale, 1.0f)
                renderQuad(stack, buffer, -0.3f, 0.3f, 0f, 0.8f, -0.3f, -0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, -0.3f, 0f, 0.8f, 0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, 0.3f, 0f, 0.8f, -0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, -0.3f, -0.3f, 0f, 0.8f, 0.3f, -0.3f, sprite)
            }
            Direction.NORTH -> {
                stack.mulPose(Quaternion.fromXYZ(Math.toRadians(90.0).toFloat(), 0f, 0f))
                stack.translate(0.0, -1.25, -1.25)
                stack.scale(1.0f, scale, 1.0f)
                renderQuad(stack, buffer, -0.3f, 0.3f, 0f, 0.8f, -0.3f, -0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, -0.3f, 0f, 0.8f, 0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, 0.3f, 0f, 0.8f, -0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, -0.3f, -0.3f, 0f, 0.8f, 0.3f, -0.3f, sprite)
            }
            Direction.SOUTH -> {
                stack.mulPose(Quaternion.fromXYZ(Math.toRadians(270.0).toFloat(), 0f, 0f))
                stack.translate(0.0, -1.25, 1.25)
                stack.scale(1.0f, scale, 1.0f)
                renderQuad(stack, buffer, -0.3f, 0.3f, 0f, 0.8f, -0.3f, -0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, -0.3f, 0f, 0.8f, 0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, 0.3f, 0f, 0.8f, -0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, -0.3f, -0.3f, 0f, 0.8f, 0.3f, -0.3f, sprite)
            }
            Direction.WEST -> {
                stack.mulPose(Quaternion.fromXYZ(0f, 0f, Math.toRadians(270.0).toFloat()))
                stack.translate(-1.25, -1.25, 0.0)
                stack.scale(1.0f, scale, 1.0f)
                renderQuad(stack, buffer, -0.3f, 0.3f, 0f, 0.8f, -0.3f, -0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, -0.3f, 0f, 0.8f, 0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, 0.3f, 0f, 0.8f, -0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, -0.3f, -0.3f, 0f, 0.8f, 0.3f, -0.3f, sprite)
            }
            Direction.EAST -> {
                stack.mulPose(Quaternion.fromXYZ(0f, 0f, Math.toRadians(90.0).toFloat()))
                stack.translate(1.25, -1.25, 0.0)
                stack.scale(1.0f, scale, 1.0f)
                renderQuad(stack, buffer, -0.3f, 0.3f, 0f, 0.8f, -0.3f, -0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, -0.3f, 0f, 0.8f, 0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, 0.3f, 0.3f, 0f, 0.8f, -0.3f, 0.3f, sprite)
                renderQuad(stack, buffer, -0.3f, -0.3f, 0f, 0.8f, 0.3f, -0.3f, sprite)
            }
        }

        stack.popPose()
    }

    override fun shouldRenderOffScreen(pBlockEntity: BoxTile): Boolean {
        return true
    }

    override fun getViewDistance(): Int {
        return 256
    }

    override fun shouldRender(pBlockEntity: BoxTile, pCameraPos: Vec3): Boolean {
        return true
    }
}