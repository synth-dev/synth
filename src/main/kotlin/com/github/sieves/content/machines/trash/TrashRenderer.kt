package com.github.sieves.content.machines.trash

import com.github.sieves.api.*
import com.github.sieves.content.io.link.*
import com.github.sieves.registry.Registry.Items
import com.github.sieves.registry.Registry.Net
import com.github.sieves.registry.internal.net.*
import com.google.common.collect.*
import com.mojang.blaze3d.vertex.*
import com.mojang.math.*
import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.core.*
import net.minecraft.core.Direction.*
import net.minecraft.core.particles.*
import net.minecraft.sounds.*
import net.minecraft.world.InteractionHand.*
import net.minecraft.world.item.*
import net.minecraftforge.network.NetworkEvent.Context
import java.lang.Math.*
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

class TrashRenderer : ApiRenderer<TrashTile>() {

    private var localDelta = 0f

    init {

    }


    companion object {
        private val deletions = ConcurrentHashMap<BlockPos, ArrayList<TrackedItem>>()
        private val deletionQueue: Queue<TrackedItem> = Queues.newArrayDeque()

        fun netListener(packet: DeleteItemPacket, context: Context): Boolean {
            if (!packet.item.isEmpty)
                deletions.getOrPut(packet.blockPos) { ArrayList() }
                    .add(
                        TrackedItem(
                            packet.slot,
                            0.5f,
                            0.2f,
                            0.5f,
                            false,
                            packet.item,
                            packet.result
                        )
                    )

            val distance = packet.blockPos.distSqr(Minecraft.getInstance().player!!.onPos)
            var volume: Float = (3 / sqrt(distance).toFloat())
            if (sqrt(distance) > 24)
                volume = 0f
            Minecraft.getInstance().player?.playSound(
                SoundEvents.ENDERMAN_TELEPORT, volume * 0.8f, random().toFloat()
            )

            return true
        }

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

    override fun render(
        entity: TrashTile, partial: Float, stack: PoseStack, buffer: MultiBufferSource, packed: Int, overlay: Int
    ) {


        val toDelete = deletions[entity.blockPos] ?: return
        if (toDelete.isEmpty()) return
        stack.pushPose()
        stack.translate(0.19, -0.62, 0.19)

        stack.scale(0.62f, 1.0f, 0.62f)
        val matrix = stack.last().pose()
        renderFace(
            matrix,
            buffer.getBuffer(RenderType.endPortal()),
            0.0f,
            1.0f,
            0.75f,
            0.75f,
            1.0f,
            1.0f,
            0.0f,
            0.0f,
        )
        stack.popPose()
        stack.pushPose()
        RenderType.translucent()
        stack.translate(0.09, entity.getHeight().toDouble() + 0.92, 0.91)
        stack.mulPose(Vector3f.XP.rotationDegrees(180f))
        stack.scale(0.82f, 0.82f, 0.82f)
        renderFace(
            stack.last().pose(),
            buffer.getBuffer(RenderType.endPortal()),
            0.0f,
            1.0f,
            0.375f,
            0.375f,
            1.0f,
            1.0f,
            0.0f,
            0.0f,
        )
        val front =
            renderFace(
                stack.last().pose(),
                buffer.getBuffer(RenderType.endPortal()),
                0.0f,
                1.0f,
                0.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f
            )
        renderFace(
            stack.last().pose(),
            buffer.getBuffer(RenderType.endPortal()),
            0.0f,
            1.0f,
            1.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )
        renderFace(
            stack.last().pose(),
            buffer.getBuffer(RenderType.endPortal()),
            1.0f,
            1.0f,
            1.0f,
            0.0f,
            0.0f,
            1.0f,
            1.0f,
            0.0f
        )
        renderFace(
            stack.last().pose(),
            buffer.getBuffer(RenderType.endPortal()),
            0.0f,
            0.0f,
            0.0f,
            1.0f,
            0.0f,
            1.0f,
            1.0f,
            0.0f
        )

        stack.popPose()

        val timeScale = 0.0025f
        localDelta = (Minecraft.getInstance().deltaFrameTime * timeScale)
        for (item in toDelete)
            updateAndRenderDeleted(stack, entity, localDelta, buffer, item)
        while (!deletionQueue.isEmpty()) {
            toDelete.remove(deletionQueue.poll())
        }
    }

    private fun renderFlame(
        entity: TrashTile,
        item: TrackedItem,
        pos: Vector3f,
        startX: Float,
        startZ: Float,
        step: Float,
        effect: ParticleOptions
    ) {
        val pos = Vector3f(item.x, item.y + 0.25f, item.z)
        val startY = Math.round(item.y).toDouble()
        val startVec = Vector3f(startX.toFloat(), 0.9f, startZ.toFloat())
        if (startY >= 1) {
            startVec.lerp(pos, 0.1f)
            Minecraft.getInstance().level?.addParticle(
                effect,
                ((entity.blockPos.x + startVec.x()).toDouble()),
                (entity.blockPos.y + startVec.y()).toDouble(),
                ((entity.blockPos.z + startVec.z()).toDouble()),
                0.0,
                0.0,
                0.0
            )
            for (i in 0 until  100) {
                startVec.lerp(pos, (i / 100f))
                Minecraft.getInstance().level?.addParticle(
                    effect,
                    ((entity.blockPos.x + startVec.x()).toDouble()),
                    (entity.blockPos.y + startVec.y()).toDouble(),
                    ((entity.blockPos.z + startVec.z()).toDouble()),
                    0.0,
                    0.0,
                    0.0
                )
            }
        }
    }

    private fun updateAndRenderDeleted(
        stack: PoseStack,
        entity: TrashTile,
        delta: Float,
        buffer: MultiBufferSource,
        item: TrackedItem
    ) {

        val distance = entity.blockPos.distSqr(Minecraft.getInstance().player!!.onPos)
        var volume: Float = (3 / sqrt(distance).toFloat())
        if (sqrt(distance) > 24)
            volume = 0f
        item.deletedTime += delta
        val radius = 0.2
        val pos = Vector3f(item.x, item.y, item.z)
        val x = (cos(toRadians((item.deletedTime * 360.0) % 360)) * radius)
        val z = (sin(toRadians((item.deletedTime * 360.0) % 360)) * radius)
        pos.lerp(
            Vector3f((0.5f - x).toFloat(), item.deletedTime * (entity.getHeight()) + 0.5f, (0.5f - z).toFloat()),
            item.deletedTime
        )


        item.x = (pos.x())
        item.y = pos.y()
        item.z = (pos.z())

//        val startX = 0
//        val startY = floor(item.y).toDouble()
//        val startZ = 0
//        val startVec = Vector3f(startX.toFloat(), startY.toFloat(), startZ.toFloat())
//        startVec.lerp(pos, 0.2f)
//        if (startY >= 1) {
//            Minecraft.getInstance().level?.addParticle(
//                ParticleTypes.SOUL_FIRE_FLAME,
//                ((entity.blockPos.x + startVec.x()).toDouble()),
//                (entity.blockPos.y + startVec.y()).toDouble(),
//                ((entity.blockPos.z + startVec.z()).toDouble()),
//                0.0,
//                0.0,
//                0.0
//            )
//            startVec.lerp(pos, 1f)
//            Minecraft.getInstance().level?.addParticle(
//                ParticleTypes.SOUL_FIRE_FLAME,
//                ((entity.blockPos.x + startVec.x()).toDouble()),
//                (entity.blockPos.y + startVec.y()).toDouble(),
//                ((entity.blockPos.z + startVec.z()).toDouble()),
//                0.0,
//                0.0,
//                0.0
//            )
//        }
        if ((item.deletedTime * 100).toInt() % 20 == 1) {
            if (Math.random() < 0.5)
                renderFlame(entity, item, pos, 0.1f, 0.1f, 1f, ParticleTypes.REVERSE_PORTAL)
            if (Math.random() > 0.6)
                renderFlame(entity, item, pos, 0.9f, 0.9f, 1f, ParticleTypes.REVERSE_PORTAL)
            if (Math.random() > 0.8)
                renderFlame(entity, item, pos, 0.1f, 0.9f, 1f, ParticleTypes.REVERSE_PORTAL)
            if (Math.random() > 0.43)
                renderFlame(entity, item, pos, 0.9f, 0.1f, 1f, ParticleTypes.REVERSE_PORTAL)
            Minecraft.getInstance().player?.playSound(
                SoundEvents.EVOKER_CELEBRATE,
                volume * 0.5f,
                0.8f
            )
        }
        val blockPos = BlockPos(
            item.x.toInt(),
            (item.deletedTime * (entity.getHeight()) + 0.5f).roundToInt(),
            item.x.toInt()
        )

        Minecraft.getInstance().level?.addParticle(
            ParticleTypes.REVERSE_PORTAL,
            (blockPos.x + pos.x()).toDouble(),
            (blockPos.y + pos.y()).toDouble(),
            (blockPos.z + pos.z()).toDouble(),
            0.0,
            0.0,
            0.0
        )


        if (item.deletedTime <= 0.5) {
            if ((item.deletedTime * 100).toInt() % 7 == 1 && Minecraft.getInstance().player?.getItemInHand(MAIN_HAND)?.item == Items.Linker)
                Minecraft.getInstance().level?.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    (entity.blockPos.x + pos.x()).toDouble(),
                    (entity.blockPos.y + pos.y()).toDouble(),
                    (entity.blockPos.z + pos.z()).toDouble(),
                    0.0,
                    0.0,
                    0.0
                )

        } else {

            if (!item.targetItem.isEmpty && !item.item.sameItem(item.targetItem)) {
                item.item = item.targetItem
                Minecraft.getInstance().player?.playSound(
                    SoundEvents.ENDER_DRAGON_GROWL,
                    0.5f * volume,
                    (random()).toFloat() * 0.1f
                )
                val vector = Vector3f(
                    random().toFloat() * 0.8f - 0.5f,
                    random().toFloat() * 0.8f - 0.5f,
                    random().toFloat() * 0.8f - 0.5f
                )
                for (i in 0 until 200)
                    if (random() >= 0.5)
                        pos.sub(vector)
                    else pos.sub(vector)
                Minecraft.getInstance().level?.addParticle(
                    ParticleTypes.ASH,
                    (entity.blockPos.x + pos.x()).toDouble(),
                    (entity.blockPos.y + pos.y() + 0.8f).toDouble(),
                    (entity.blockPos.z + pos.z()).toDouble(),
                    0.0,
                    0.0,
                    0.0
                )
            }

        }


        stack.pushPose()
        stack.translate(item.x.toDouble(), item.y.toDouble(), item.z.toDouble())
        stack.mulPose(Vector3f.YP.rotationDegrees(item.deletedTime * 360))
        renderItem(stack, buffer, item.item)
        stack.popPose()
        if (item.deletedTime >= .999) {
            if (!item.item.sameItem(item.targetItem)) {
                deletionQueue.add(item)
                Minecraft.getInstance().level?.addParticle(
                    ParticleTypes.POOF,
                    (entity.blockPos.x + item.x).toDouble(),
                    (entity.blockPos.y + item.y).toDouble(),
                    (entity.blockPos.z + item.z).toDouble(),
                    0.0,
                    0.0,
                    0.0
                )
                Minecraft.getInstance().player?.playSound(
                    SoundEvents.EVOKER_CELEBRATE,
                    volume * 0.5f,
                    0.8f
                )
            } else {
                Minecraft.getInstance().level?.addParticle(
                    ParticleTypes.FLASH,
                    (entity.blockPos.x + item.x).toDouble(),
                    (entity.blockPos.y + item.y).toDouble(),
                    (entity.blockPos.z + item.z).toDouble(),
                    0.0,
                    0.0,
                    0.0
                )
            }
        }

    }

    //val radius = 0.2
    //val x = 0.5 + (cos(Math.toRadians(angle)) * radius)
    //val y = 0.05 * slot
    //val z = 0.5 + (sin(Math.toRadians(angle)) * radius)
    //item.x = x.toFloat()
    //item.y = y.toFloat()
    //item.z = z.toFloat()

    private data class TrackedItem(
        val slot: Int,
        var x: Float,
        var y: Float,
        var z: Float,
        var deleted: Boolean = false,
        var item: ItemStack,
        var targetItem: ItemStack,
        var deletedTime: Float = 0f
    )

}