package com.github.sieves.content.machines.trash

import com.github.sieves.api.*
import com.github.sieves.content.io.link.*
import com.github.sieves.registry.Registry.Items
import com.github.sieves.registry.Registry.Net
import com.github.sieves.registry.internal.net.*
import com.google.common.collect.*
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.*
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
import kotlin.collections.HashMap
import kotlin.math.*

class TrashRenderer : ApiRenderer<TrashTile>() {
    private val containers = HashMap<BlockPos, TrashScreen>()
    private val buffer: RenderTarget = TextureTarget(512, 512, false, true)

    fun renderFace(
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

//        val screen = containers.getOrPut(entity.blockPos) {
//            TrashScreen(
//                TrashContainer(entity.blockPos.hashCode(), Minecraft.getInstance().player!!.inventory, entity.blockPos),
//                Minecraft.getInstance().player!!.inventory
//            )
//        }
//        this.buffer.bindWrite(true)
//        val pose = RenderSystem.getModelViewStack()
//        this.buffer.unbindWrite()
////        this.buffer

    }

}