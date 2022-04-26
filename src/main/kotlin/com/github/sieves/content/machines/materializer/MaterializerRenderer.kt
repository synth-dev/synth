package com.github.sieves.content.machines.materializer

import com.github.sieves.api.*
import com.github.sieves.registry.Registry.Net
import com.github.sieves.registry.internal.net.*
import com.google.common.collect.*
import com.mojang.blaze3d.vertex.*
import com.mojang.math.*
import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.block.model.*
import net.minecraft.client.renderer.texture.*
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraftforge.network.NetworkEvent
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class MaterializerRenderer : ApiRenderer<MaterializerTile>() {
    companion object {
        private val active: MutableMap<BlockPos, Craft> = HashMap()
        private val removalQueue: MutableSet<BlockPos> = HashSet()

        init {
            Net.MaterializerStart.clientListener(::onStartPacket)
            Net.MaterializerStop.clientListener(::onStopPacket)
        }

        private fun onStartPacket(packet: StartMaterializer, context: NetworkEvent.Context): Boolean {
            active[packet.blockPos] = Craft(
                packet.craft.input,
                if (packet.craft.output.isEmpty()) ItemStack.EMPTY else packet.craft.output.first(),
                0f,
                0f,
                Vector3f(0.5f, 0.5f, 0.5f)
            )
            return true
        }

        private fun onStopPacket(packet: StopMaterializer, context: NetworkEvent.Context): Boolean {
            removalQueue.add(packet.blockPos)
            return true
        }
    }


    override fun render(
        pBlockEntity: MaterializerTile,
        pPartialTick: Float,
        pPoseStack: PoseStack,
        pBufferSource: MultiBufferSource,
        pPackedLight: Int,
        pPackedOverlay: Int
    ) {
        if (!active.containsKey(pBlockEntity.blockPos)) return
        val craft = active[pBlockEntity.blockPos]!!

//        if (removalQueue.contains(pBlockEntity.blockPos))
//            renderRemoval(pPoseStack, pBufferSource, pBlockEntity, craft)
//        else
        renderOrbit(pPoseStack, pBufferSource, pBlockEntity, craft)
    }

    private fun renderFace(
        pPose: Matrix4f,
        pConsumer: VertexConsumer,
        pX0: Float,
        pX1: Float,
        pY0: Float,
        pY1: Float,
        pZ0: Float,
        pZ1: Float,
        pZ2: Float,
        pZ3: Float,
    ) {
        pConsumer.vertex(pPose, pX0, pY0, pZ0).endVertex()
        pConsumer.vertex(pPose, pX1, pY0, pZ1).endVertex()
        pConsumer.vertex(pPose, pX1, pY1, pZ2).endVertex()
        pConsumer.vertex(pPose, pX0, pY1, pZ3).endVertex()
    }

    private fun renderOrbit(stack: PoseStack, bufferSource: MultiBufferSource, tile: MaterializerTile, craft: Craft) {
        val craft = tile.craft
        var time = tile.craft.currentTime / (tile.craft.time.toFloat() / tile.getConfig().speedModifier)
        var itemstack = craft.input
        val start = Vector3f(tile.blockPos.x + 0.5f, tile.blockPos.y + 1f, tile.blockPos.z + 0.5f)
        start.lerp(
            Vector3f(
                tile.blockPos.x + 0.5f,
                tile.blockPos.y + time + 1.2f,
                tile.blockPos.z + 0.5f,
            ),
            time
        )



        if (time > 0.9f) {
            itemstack = craft.output.firstOrNull() ?: ItemStack.EMPTY
            if (itemstack.isEmpty) {
                if (Math.random() > 0.98)
                    tile.level?.addParticle(
                        ParticleTypes.CLOUD,
                        tile.blockPos.x.toDouble() + 0.5,
                        tile.blockPos.y + 2.0 * time,
                        tile.blockPos.z.toDouble() + 0.5,
                        0.0,
                        0.0,
                        0.0
                    )
            } else {
                stack.pushPose()
                stack.translate(0.0, 2.65, 1.0)
                stack.mulPose(Vector3f.XP.rotationDegrees(180f))

                stack.scale(1f, 1.0f, 1f)
                val matrix = stack.last().pose()
                renderFace(
                    matrix,
                    bufferSource.getBuffer(RenderType.endPortal()),
                    0.0f,
                    1.0f,
                    0.375f,
                    0.375f,
                    1.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                )
                stack.popPose()
                if (Math.random() > 0.80)
                    tile.level?.addParticle(
                        ParticleTypes.ASH,
                        tile.blockPos.x.toDouble() + 0.5,
                        tile.blockPos.y + 2.0 * time,
                        tile.blockPos.z.toDouble() + 0.5,
                        0.0,
                        0.0,
                        0.0
                    )
            }

        } else {
            if (Math.random() > 0.90) {
                tile.level?.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    start.x().toDouble(),
                    start.y().toDouble(),
                    start.z().toDouble(),
                    0.0,
                    0.0,
                    0.0
                )
            }
        }
        stack.pushPose()
        stack.translate(0.5, (1.0f + time).toDouble(), 0.5)
        stack.mulPose(Vector3f.YP.rotationDegrees(time * 360f))
//        var scaleX = ((System.currentTimeMillis() / 10) % 100) / 100f
//        var scaleY = ((System.currentTimeMillis() / 15) % 125) / 120f
//        var scaleZ = ((System.currentTimeMillis() / 5) % 96) / 96f
//        stack.scale(scaleX, scaleY, scaleZ)
//        stack.translate(craft.position.x().toDouble(), craft.position.y().toDouble(), craft.position.z().toDouble())
        Minecraft.getInstance().itemRenderer.renderStatic(
            itemstack,
            ItemTransforms.TransformType.GROUND,
            15728880,
            OverlayTexture.NO_OVERLAY,
            stack,
            bufferSource,
            0

        )
        stack.popPose()


    }

    private fun renderRemoval(stack: PoseStack, bufferSource: MultiBufferSource, tile: MaterializerTile, craft: Craft) {
//        if (craft.removeTime >= 1.0f) {
        removalQueue.remove(tile.blockPos)
        active.remove(tile.blockPos)
//        } else {
//            craft.removeTime += Minecraft.getInstance().deltaFrameTime * 0.5f
//        }
    }

    private data class Craft(
        val input: ItemStack,
        val output: ItemStack,
        var time: Float,
        var removeTime: Float,
        val position: Vector3f
    ) {

    }
}