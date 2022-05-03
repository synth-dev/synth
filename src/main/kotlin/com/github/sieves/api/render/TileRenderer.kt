package com.github.sieves.api.render

import com.mojang.blaze3d.vertex.*
import com.mojang.math.*
import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.block.model.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.client.renderer.texture.*
import net.minecraft.core.*
import net.minecraft.core.particles.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.phys.*
import software.bernie.geckolib3.core.*
import software.bernie.geckolib3.geo.render.built.*
import software.bernie.geckolib3.renderers.geo.*

abstract class TileRenderer<T>(
    //Pass the context to the renderer
    context: BlockEntityRendererProvider.Context,
    //The tile entity class used to compute the resource name. Tile entity class name must match json file names.
    //the class name can end with tile and have proper capitalization and no underscores. The class's name will be transformed.
    protected val modelProvider: TileModel<T>,
) : GeoBlockRenderer<T>(context, modelProvider) where T : BlockEntity, T : IAnimatable {

    /**
     * Add custom rendering code here outside geckolib's animation system
     */
    protected abstract fun renderFirst(tile: T, model: GeoModel, stack: PoseStack, buffer: MultiBufferSource, packedLight: Int)
    protected abstract fun renderLast(tile: T, model: GeoModel, stack: PoseStack, buffer: MultiBufferSource, packedLight: Int)

    override fun renderEarly(
        animatable: T,
        stackIn: PoseStack?,
        ticks: Float,
        renderTypeBuffer: MultiBufferSource?,
        vertexBuilder: VertexConsumer?,
        packedLightIn: Int,
        packedOverlayIn: Int,
        red: Float,
        green: Float,
        blue: Float,
        partialTicks: Float
    ) {
        val model = modelProvider.getModel(modelProvider.getModelLocation(animatable))
        if (stackIn != null && renderTypeBuffer != null)
            renderFirst(animatable, model, stackIn, renderTypeBuffer, packedLightIn)
    }

    override fun renderLate(
        animatable: T,
        stackIn: PoseStack?,
        ticks: Float,
        renderTypeBuffer: MultiBufferSource?,
        vertexBuilder: VertexConsumer?,
        packedLightIn: Int,
        packedOverlayIn: Int,
        red: Float,
        green: Float,
        blue: Float,
        partialTicks: Float
    ) {
        val model = modelProvider.getModel(modelProvider.getModelLocation(animatable))
        if (stackIn != null && renderTypeBuffer != null)
            renderLast(animatable, model, stackIn, renderTypeBuffer, packedLightIn)
    }


    protected fun renderBeam(start: BlockPos, stop: BlockPos, offset: Vector3f, delta: Float) {
        val target = Vector3f(
            (stop.x - start.x).toFloat(),
            (stop.y - start.y).toFloat(),
            (stop.z - start.z).toFloat()
        )
        offset.lerp(target, delta)
        Minecraft.getInstance().level?.addParticle(
            ParticleTypes.REVERSE_PORTAL,
            start.x + 0.5 + offset.x(),
            start.y + 0.5 + offset.y(),
            start.z + 0.5 + offset.z(),
            0.0,
            0.0,
            0.0
        )
    }

    protected fun renderItem(stack: PoseStack, bufferSource: MultiBufferSource, items: ItemStack) {
        Minecraft.getInstance().itemRenderer.renderStatic(
            items,
            ItemTransforms.TransformType.GROUND,
            15728880,
            OverlayTexture.NO_OVERLAY,
            stack,
            bufferSource,
            0

        )
    }

    override fun getViewDistance(): Int {
        return 256
    }

    override fun shouldRenderOffScreen(pBlockEntity: BlockEntity): Boolean {
        return true
    }

    override fun shouldRender(pBlockEntity: BlockEntity, pCameraPos: Vec3): Boolean {
        return true
    }

}