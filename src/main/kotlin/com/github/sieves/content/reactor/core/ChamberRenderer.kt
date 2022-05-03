package com.github.sieves.content.reactor.core

import com.github.sieves.api.render.*
import com.github.sieves.api.render.TileModel.Companion.of
import com.mojang.blaze3d.vertex.*
import com.mojang.math.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.resources.*
import software.bernie.geckolib3.geo.render.built.*
import kotlin.math.*

class ChamberRenderer(ctx: BlockEntityRendererProvider.Context) : TileRenderer<ChamberTile>(ctx, TileModel of ChamberTile::class) {

    /**
     * Add custom rendering code here outside geckolib's animation system
     */
    override fun renderFirst(tile: ChamberTile, model: GeoModel, stack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {
        val test = model.getBone("root")
        if (test.isPresent) {
            val bone = test.get()
            val rot = Math.toDegrees(abs(bone.rotationY).toDouble()) * 2
            if (rot != 0.0) println(rot)
            stack.pushPose()
            stack.mulPose(Vector3f.YP.rotationDegrees(rot.toFloat()))
            stack.translate(0.0, 0.8, 0.0)
            renderItem(stack, buffer, tile.items map { getStackInSlot(0) })
            stack.popPose()
        }
    }


    override fun renderLast(tile: ChamberTile, model: GeoModel, stack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {

    }

    override fun getRenderType(
        animatable: ChamberTile?,
        partialTicks: Float,
        stack: PoseStack?,
        renderTypeBuffer: MultiBufferSource?,
        vertexBuilder: VertexConsumer?,
        packedLightIn: Int,
        textureLocation: ResourceLocation?
    ): RenderType? {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}