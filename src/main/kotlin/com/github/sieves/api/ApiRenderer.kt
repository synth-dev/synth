package com.github.sieves.api

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.*
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.texture.*
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.InventoryMenu.*
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fluids.capability.templates.FluidTank


abstract class ApiRenderer<T : ApiTile<T>> : BlockEntityRenderer<T> {
    /**Adds a block at the given psotion**/
    private fun add(
        renderer: VertexConsumer,
        stack: PoseStack,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        light: Int
    ) = renderer.vertex(stack.last().pose(), x, y, z)
        .color(r, g, b, a)
        .uv(u, v)
        .uv2(light)
        .normal(1.0f, 0f, 0f)
        .endVertex()

    /**Renders fluid **/
    fun renderLiquid(
        stack: PoseStack,
        buffer: MultiBufferSource,
        tank: FluidTank,
        min: FloatArray,
        max: FloatArray,
        lightLevel: Int,
        rotation: Quaternion,
        opacity: Float,
        fluidScale: Float,
        ) {
        val block = LightTexture.block(lightLevel)
        val sky = LightTexture.sky(lightLevel)
        val normalized = (block + sky) / 29.0f
        val lightCoords = 240
        //        val lightCoords: Float = (lightLevel)


        val fluid = tank.fluid
        val renderFluid = tank.fluid.fluid ?: return

        val attribs = renderFluid.attributes
        val fluidStill = attribs.getStillTexture(fluid)

        val sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(fluidStill)
        val builder = buffer.getBuffer(RenderType.translucent())
        val color = renderFluid.attributes.color;

        val r = (color shr 16 and 0xFF) / 255.0F;
        val g = (color shr 8 and 0xFF) / 255.0F;
        val b = (color and 0xFF) / 255.0F;
        val minX: Float = (min[0] / 15.0f)
        val minY: Float = (min[1] / 15.0f)
        val minZ: Float = (min[2] / 15.0f)

        val maxX: Float = (max[0] / 15.0f)
//        val maxY: Float = (max[1] / 15.0f)
        val maxY: Float = ((((fluidScale * (max[1] / 15.0f)))) + minY) - 0.015f
        val maxZ: Float = (max[2] / 15.0f)
        stack.pushPose()

        stack.translate(.5, 0.5, .5);
        stack.mulPose(rotation);

        stack.translate(-.5, (-0.5), -.5);
//        stack.scale(scale.x(), scale.y(),scale.z)
        // Top Face
        add(builder, stack, minX, maxY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)

        // Bottom Facstack
        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)

        // Front FacestackUTH]
        add(builder, stack, maxX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)

        // Back Facesf
        add(builder, stack, maxX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)

        //            // east Facestack
        add(builder, stack, maxX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        add(builder, stack, maxX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        //            // west Faces
        add(builder, stack, minX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        add(builder, stack, minX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)


//
//        //            // east Facestack
//        add(builder, stack, maxX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
//        add(builder, stack, maxX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
//        add(builder, stack, maxX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
//        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
//
//        add(builder, stack, maxX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
//        add(builder, stack, maxX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
//        add(builder, stack, maxX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
//        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
//
//        //            // west Faces
//        add(builder, stack, minX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
//        add(builder, stack, minX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
//        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
//        add(builder, stack, minX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
//
//        add(builder, stack, minX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
//        add(builder, stack, minX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
//        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
//        add(builder, stack, minX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        stack.popPose()
    }


    /**Renders fluid **/
    fun renderLiquid(
        stack: PoseStack,
        buffer: MultiBufferSource,
        tank: FluidTank,
        min: FloatArray,
        max: FloatArray,
        lightLevel: Int,
        rotation: Quaternion,
        opacity: Float,
    ) {
        val block = LightTexture.block(lightLevel)
        val sky = LightTexture.sky(lightLevel)
        val normalized = (block + sky) / 29.0f
        val lightCoords = 240
        //        val lightCoords: Float = (lightLevel)


        val fluid = tank.fluid
        val renderFluid = tank.fluid.fluid ?: return

        val attribs = renderFluid.attributes
        val fluidStill = attribs.getStillTexture(fluid)

        val sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(fluidStill)
        val builder = buffer.getBuffer(RenderType.translucent())
        val color = renderFluid.attributes.color;

        val r = (color shr 16 and 0xFF) / 255.0F;
        val g = (color shr 8 and 0xFF) / 255.0F;
        val b = (color and 0xFF) / 255.0F;
        val minX: Float = (min[0] / 15.0f)
        val minY: Float = (min[1] / 15.0f)
        val minZ: Float = (min[2] / 15.0f)

        val maxX: Float = (max[0] / 15.0f)
        val maxY: Float = (max[1] / 15.0f)
//        val maxY: Float = ((((fluidScale * (max[1] / 15.0f)))) + minY) - 0.015f
        val maxZ: Float = (max[2] / 15.0f)
        stack.pushPose()

        stack.translate(.5, 0.5, .5);
        stack.mulPose(rotation);
        stack.translate(-.5, (-0.5), -.5);
        add(builder, stack, minX, maxY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)

        // Bottom Facstack
        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)

        // Front FacestackUTH]
        add(builder, stack, maxX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)

        // Back Facesf
        add(builder, stack, maxX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)

        //            // east Facestack
        add(builder, stack, maxX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        add(builder, stack, maxX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, maxX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        //            // west Faces
        add(builder, stack, minX, maxY, maxZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, minZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        add(builder, stack, minX, maxY, minZ, sprite.u0, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, maxY, maxZ, sprite.u1, sprite.v0, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, maxZ, sprite.u1, sprite.v1, r, g, b, opacity, lightCoords)
        add(builder, stack, minX, minY, minZ, sprite.u0, sprite.v1, r, g, b, opacity, lightCoords)

        stack.popPose()
    }


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
        val stack = stack.last().pose()
        buffer.vertex(stack, startX, startY, startZ)
            .color(r, g, b, 1.0f)
            .normal(1f, 0f, 0f)
            .endVertex()
        buffer.vertex(stack, stopX, stopY, stopZ)
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
        val stack = stack.last().pose()
        buffer.vertex(stack, startX, startY, startZ)
            .color(1.0f, 1.0f, 1.0f, alpha)
            .uv(sprite.u0, sprite.v0)
            .uv2(brightness)
            .normal(1f, 0f, 0f)
            .endVertex()
        buffer.vertex(stack, startX, stopY, startZ)
            .color(1.0f, 1.0f, 1.0f, alpha)
            .uv(sprite.u0, sprite.v1)
            .uv2(brightness)
            .normal(1f, 0f, 0f)
            .endVertex()

        buffer.vertex(stack, stopX, stopY, stopZ)
            .color(1.0f, 1.0f, 1.0f, alpha)
            .uv(sprite.u1, sprite.v1)
            .uv2(brightness)
            .normal(1f, 0f, 0f)
            .endVertex()
        buffer.vertex(stack, stopX, startY, stopZ)
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