package com.github.sieves.api.tile

import com.github.sieves.content.reactor.control.*
import com.github.sieves.util.*
import net.minecraft.core.BlockPos
import net.minecraft.core.BlockPos.*
import net.minecraft.nbt.*
import net.minecraft.network.*
import net.minecraft.network.protocol.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.BlockState

abstract class ReactorTile<T : BlockEntity>(type: BlockEntityType<T>, blockPos: BlockPos, blockState: BlockState) : BlockEntity(type, blockPos, blockState),
    IMultiBlock<T> {
    /**
     * The controller
     */
    override var ctrlPos: BlockPos = ZERO

    private var cachedController: ControlTile? = null

    /**
     * The optional value of the controller
     */
    override val ctrl: Opt<ControlTile>
        get() {
            if (cachedController != null && cachedController?.isRemoved == false) {
                return Opt.of(cachedController!!)
            }
            val level = self.level ?: return Opt.empty()
            val be = level.getBlockEntity(ctrlPos)
            if (be is ControlTile) {
                cachedController = be
                return Opt.of(be)
            }
            return Opt.empty()
        }

    /**
     * Push update to client
     */
    override fun update() {
        setChanged()
        if (level != null) {
            level!!.sendBlockUpdated(worldPosition, blockState, blockState, 3)
        }
    }

    override fun saveAdditional(pTag: CompoundTag) {
        super.saveAdditional(pTag)
        onSave(pTag)
        pTag.putBlockPos("controller", ctrlPos)
    }

    override fun load(pTag: CompoundTag) {
        super.load(pTag)
        onLoad(pTag)
        ctrlPos = (pTag.getBlockPos("controller").bp)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(): CompoundTag {
        return serializeNBT()
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        onInvalidate()
    }

    override fun handleUpdateTag(tag: CompoundTag) {
        super.handleUpdateTag(tag)
        load(tag)
    }

    override fun onDataPacket(net: Connection, pkt: ClientboundBlockEntityDataPacket) {
        super.onDataPacket(net, pkt)
        handleUpdateTag(pkt.tag!!)
    }

    object Ticker : BlockEntityTicker<ReactorTile<*>> {
        override fun tick(pLevel: Level, pPos: BlockPos, pState: BlockState, pBlockEntity: ReactorTile<*>) {
            if (pLevel.isClientSide) pBlockEntity.onClientTick()
            else pBlockEntity.onServerTick()
        }
    }
}