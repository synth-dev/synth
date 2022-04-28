package com.github.sieves.content.machines.core

import com.github.sieves.api.*
import com.github.sieves.content.io.fluids.*
import com.github.sieves.registry.internal.net.*
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Vector3f
import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.*
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.*
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.*
import net.minecraftforge.network.*

class CoreRenderer(private val context: Context) : ApiRenderer<CoreTile>() {

    override fun render(
        entity: CoreTile, partial: Float, stack: PoseStack, buffer: MultiBufferSource, packed: Int, overlay: Int
    ) {
        if (entity.tankTwo.fluidAmount > 0) {
            stack.pushPose()
            renderLiquid(
                stack,
                buffer,
                entity.tankTwo,
                floatArrayOf(6f, 1f, 6f),
                floatArrayOf(9f, 4f, 9f),
                packed,
                Vector3f.YP.rotationDegrees(45f),
                1.0f,
                entity.tankTwo.fluidAmount.toFloat() / entity.tankTwo.capacity
            )
            stack.popPose()

        }
        if (entity.tank.fluidAmount > 0) {

            stack.pushPose()
//            stack.scale(1f, entity.tank.fluidAmount.toFloat() / (entity.tank.capacity), 1f)
            val rot = Vector3f.YP.rotationDegrees(45f)
            rot.mul(Vector3f.ZP.rotationDegrees(180f))
            renderLiquid(
                stack,
                buffer,
                entity.tank,
                floatArrayOf(6f, 1f, 6f),
                floatArrayOf(9f, 4f, 9f),
                packed,
                rot,
                1.0f,
                entity.tank.fluidAmount.toFloat() / (entity.tank.capacity)
            )
            stack.popPose()
        }

        if (removals.containsKey(entity.blockPos)) {
            val removal = removals[entity.blockPos]!!

            if (removal.time >= 1.0f) {
                active.remove(entity.blockPos)
                removals.remove(entity.blockPos)
                Minecraft.getInstance().level?.playLocalSound(entity.blockPos, SoundEvents.LAVA_EXTINGUISH, BLOCKS, 1f, (Math.random() + 0.5f).toFloat(), true)
            } else {
                stack.pushPose()
                stack.translate(0.5, 0.5, 0.5)
                stack.scale(removal.time * 0.5f, removal.time * 0.5f, removal.time * 0.5f)
                stack.translate(0.0, -0.18, 0.0)
                renderItem(stack, buffer, removal.itemStack)
                stack.popPose()
                removal.time += Minecraft.getInstance().deltaFrameTime * 0.075f

            }
        }

        if (active.containsKey(entity.blockPos)) {
            val active = active[entity.blockPos]!!
            renderLiquid(
                stack,
                buffer,
                entity.tankTwo,
                floatArrayOf(7f, 5.1f, 7f),
                floatArrayOf(8f, (5.1 + active.time).toFloat(), 8f),
                packed,
                Vector3f.YP.rotationDegrees((System.currentTimeMillis() / 5 % 360).toFloat()),
                1.0f
            )
            renderLiquid(
                stack,
                buffer,
                entity.tank,
                floatArrayOf(7f, (9.9f - active.time), 7f),
                floatArrayOf(8f, 9.9f, 8f),
                packed,
                Vector3f.YP.rotationDegrees((System.currentTimeMillis() / 5 % 360).toFloat()),
                1.0f
            )
            if (active.time > 2.38)
                active.time = 2.38f
            active.time += Minecraft.getInstance().deltaFrameTime * 0.040f
        }
    }


    companion object {
        private val active = HashMap<BlockPos, Active>()
        private val removals = HashMap<BlockPos, Removal>()

        fun onStart(onstart: StartSolidifer, context: NetworkEvent.Context): Boolean {
            active[onstart.blockPos] = Active(0f)
            return true
        }

        fun onStop(onstart: StopSolidifer, context: NetworkEvent.Context): Boolean {
            removals[onstart.blockPos] = Removal(onstart.item, 0f)
            return true
        }
    }

    private data class Removal(val itemStack: ItemStack, var time: Float)
    private data class Active(var time: Float)

    override fun shouldRenderOffScreen(pBlockEntity: CoreTile): Boolean {
        return true
    }

    override fun getViewDistance(): Int {
        return 256
    }

    override fun shouldRender(pBlockEntity: CoreTile, pCameraPos: Vec3): Boolean {
        return true
    }
}