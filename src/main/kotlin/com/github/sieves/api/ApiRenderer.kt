package com.github.sieves.api

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.*
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.texture.*
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack


abstract class ApiRenderer<T : ApiTile<T>> : BlockEntityRenderer<T> {
    protected fun renderLine(
        stack: PoseStack,
        buffer: VertexConsumer,
        startX: Float,
        startY: Float,
        startZ: Float,
        stopX: Float,
        stopY: Float,
        stopZ: Float,
        r: Float = 1.0f,
        g: Float = 1.0f,
        b: Float = 1.0f
    ) {
        val matrix = stack.last().pose()
        buffer.vertex(matrix, startX, startY, startZ)
            .color(r, g, b, 1.0f)
            .normal(1f, 0f, 0f)
            .endVertex()
        buffer.vertex(matrix, stopX, stopY, stopZ)
            .color(r, g, b, 1.0f)
            .normal(1f, 0f, 0f)
            .endVertex()
    }

    protected fun renderQuad(
        stack: PoseStack,
        buffer: VertexConsumer,
        startX: Float,
        stopX: Float,
        startY: Float,
        stopY: Float,
        startZ: Float,
        stopZ: Float,
        sprite: TextureAtlasSprite,
        alpha: Float = 1.0f
    ) {
        val brightness = LightTexture.FULL_BRIGHT
        val matrix = stack.last().pose()
        buffer.vertex(matrix, startX, startY, startZ)
            .color(1.0f, 1.0f, 1.0f, alpha)
            .uv(sprite.u0, sprite.v0)
            .uv2(brightness)
            .normal(1f, 0f, 0f)
            .endVertex()
        buffer.vertex(matrix, startX, stopY, startZ)
            .color(1.0f, 1.0f, 1.0f, alpha)
            .uv(sprite.u0, sprite.v1)
            .uv2(brightness)
            .normal(1f, 0f, 0f)
            .endVertex()

        buffer.vertex(matrix, stopX, stopY, stopZ)
            .color(1.0f, 1.0f, 1.0f, alpha)
            .uv(sprite.u1, sprite.v1)
            .uv2(brightness)
            .normal(1f, 0f, 0f)
            .endVertex()
        buffer.vertex(matrix, stopX, startY, stopZ)
            .color(1.0f, 1.0f, 1.0f, alpha)
            .uv(sprite.u1, sprite.v0)
            .uv2(brightness)
            .normal(1f, 0f, 0f)
            .endVertex()
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

    protected fun getSprite(resourceLocation: ResourceLocation): TextureAtlasSprite =
        Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(resourceLocation)

}