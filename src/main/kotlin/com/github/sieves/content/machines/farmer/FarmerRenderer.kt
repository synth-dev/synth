package com.github.sieves.content.machines.farmer

import com.github.sieves.api.ApiRenderer
import com.github.sieves.content.machines.forester.ForesterTile
import com.github.sieves.registry.Registry
import com.github.sieves.util.length
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

class FarmerRenderer : ApiRenderer<FarmerTile>() {
    private val growthRemovals = ArrayList<Growth>()
    private val harvestRemovals = ArrayList<Harvest>()
    private val growths = HashMap<BlockPos, ArrayList<Growth>>()
    private val harvests = HashMap<BlockPos, ArrayList<Harvest>>()
    init{
        Registry.Net.GrowBlock.clientListener { growBlockPacket, _ ->

            growths.getOrPut(growBlockPacket.ownerPos) { ArrayList() }.add(Growth(growBlockPacket.blockPos, 0f))
            true
        }
        Registry.Net.HarvestBlock.clientListener { growBlockPacket, _ ->
            harvests.getOrPut(growBlockPacket.ownerPos) { ArrayList() }
                .add(Harvest(growBlockPacket.blockPos, 0f, growBlockPacket.harvested))
            true
        }
    }


    override fun render(
        pBlockEntity: FarmerTile,
        pPartialTick: Float,
        stack: PoseStack,
        pBufferSource: MultiBufferSource,
        pPackedLight: Int,
        pPackedOverlay: Int
    ) {
        renderHarvests(pBlockEntity, stack, pBufferSource)
        renderGrowths(pBlockEntity)
    }

    private fun renderHarvests(pBlockEntity: FarmerTile, stack: PoseStack, bufferSource: MultiBufferSource) {
        harvestRemovals.clear()
        harvests[pBlockEntity.blockPos]?.forEach {
            it.delta += Minecraft.getInstance().deltaFrameTime * 0.05f
            if (it.delta >= 1) harvestRemovals.add(it)
        }
        harvestRemovals.forEach {
            harvests[pBlockEntity.blockPos]?.remove(it)
        }
        val target = Vector3f(
            pBlockEntity.blockPos.x.toFloat(), pBlockEntity.blockPos.y.toFloat(),
            pBlockEntity.blockPos.z.toFloat()
        )
        val offset = Vector3f()

        harvests[pBlockEntity.blockPos]?.forEach { (pos, delta, itemstack) ->
            itemstack.forEachIndexed { index, items ->
                stack.pushPose()

                val target = Vector3f(
                    (pBlockEntity.blockPos.x - pos.x).toFloat(),
                    (pBlockEntity.blockPos.y - pos.y).toFloat(),
                    (pBlockEntity.blockPos.z - pos.z).toFloat()
                )
                val length = target.length(0.01f)
                offset.lerp(target, delta)
                stack.translate(
                    offset.x().toDouble() - (target.x() - 0.5),
                    offset.y().toDouble() - (target.y() - 0.5 + (0.2 * index)),
                    offset.z().toDouble() - (target.z() - 0.5)
                )

//            target.lerp(target, delta + length)
                Minecraft.getInstance().itemRenderer.renderStatic(
                    items,
                    ItemTransforms.TransformType.GROUND,
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    stack,
                    bufferSource,
                    0

                )
                stack.popPose()
            }
        }
    }

    private fun renderGrowths(pBlockEntity: FarmerTile) {

        growthRemovals.clear()
        growths[pBlockEntity.blockPos]?.forEach {
            it.delta += Minecraft.getInstance().deltaFrameTime * 0.1f
            if (it.delta >= 1) growthRemovals.add(it)
        }


        growthRemovals.forEach {
            growths[pBlockEntity.blockPos]?.remove(it)
        }

        if (Minecraft.getInstance().player?.getItemInHand(InteractionHand.MAIN_HAND)?.item == Registry.Items.Linker)
            growths[pBlockEntity.blockPos]?.forEach { (pos, delta) ->
                renderBeam(pBlockEntity.blockPos, pos, Vector3f(0f, 0.5f, 0f), delta)
            }

    }

    private data class Growth(var blockPos: BlockPos, var delta: Float)

    private data class Harvest(var blockPos: BlockPos, var delta: Float, val harvested: ArrayList<ItemStack>)


    private fun renderGrowth(from: BlockPos, to: BlockPos) {

    }


    override fun shouldRenderOffScreen(pBlockEntity: FarmerTile): Boolean {
        return true
    }

    override fun getViewDistance(): Int {
        return 256
    }

    override fun shouldRender(pBlockEntity: FarmerTile, pCameraPos: Vec3): Boolean {
        return true
    }
}