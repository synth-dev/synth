package com.github.sieves.api.tile

import com.github.sieves.api.ApiConfig.*
import com.github.sieves.api.ApiConfig.Side.*
import com.github.sieves.util.*
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.*
import net.minecraft.network.protocol.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.common.util.LazyOptional
import java.util.function.*
import kotlin.properties.*
import kotlin.reflect.*
import kotlin.reflect.full.*

/**
 * Out base tile, all tiles in the future will implement from this
 */
abstract class BaseTile<T : BlockEntity>(type: BlockEntityType<T>, pos: BlockPos, state: BlockState) : ITile<T>, BlockEntity(type, pos, state) {
    /**Tracks the serializable handlers**/
    protected val handlers: MutableMap<String, DelegatedHandler<*>> = HashMap()

    /**
     * Updates the shits
     */
    override fun update() {
        setChanged()
        level?.sendBlockUpdated(worldPosition, blockState, blockState, 3)
    }

    /**
     * Gets a block for the given side using the horizontal offset
     */
    protected fun getNeighbor(side: Side, horizontal: Boolean = true): BlockPos {
        val forward = blockState.getValue(if (horizontal) HorizontalDirectionalBlock.FACING else DirectionalBlock.FACING)
        return blockPos.offset(
            when (side) {
                Top -> Direction.UP
                Bottom -> Direction.DOWN
                Front -> forward
                Back -> forward.opposite
                Left -> forward.counterClockWise
                Right -> forward.clockWise
            }
        )
    }

    /**
     * This will create a handler for the given class by finding the matching constructor
     */
    protected fun <T : Any> createHandler(typeIn: KClass<T>, values: List<Any>): Opt<T> {
        try {
            val ctor = typeIn.primaryConstructor ?: return Opt.nil()
            val ctorParams = ctor.parameters
            val args = ArrayList<Any>(values)
            for (i in ctorParams.indices) {
                val param = ctorParams[i]
                val type = ((param.type.classifier ?: continue) as KClass<*>)
                val projections = param.type.arguments.firstOrNull() ?: continue
                val projectedType = projections.type ?: continue
                val classifier = (projectedType.classifier ?: continue) as KClass<*>
                if ((type.isSubclassOf(Supplier::class) || type.java == Supplier::class.java) && this::class.isSubclassOf(classifier)) {
                    args.add(i, type.cast(Supplier { classifier.cast(this) }))
                }
            }
            return Opt.of(ctor.call(*(args.toTypedArray())))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Opt.nil()
    }

    /**
     * This is used delegate registration using properties
     */
    protected inline fun <reified T : Any> handlerOf(
        name: String, vararg parameters: Any
    ): ReadOnlyProperty<Any?, DelegatedHandler<T>> {
        val instanceOpt = createHandler(T::class, parameters.toList())
        if (instanceOpt.isAbsent) error("Failed to initialize delegated handler of type ${T::class.simpleName}")
        val instance = instanceOpt()
        val handler = DelegatedHandler(instance)
        this.handlers[name] = handler
        return ReadOnlyProperty { _, _ -> handler }
    }


    /**
     * Delegate the handling of our handlers
     */
    class DelegatedHandler<T : Any>(private val value: T) : Supplier<T>, () -> T {
        /**Keep track of our lazy for serialization and invalidation**/
        val lazy: LazyOptional<T> = LazyOptional.of { value }

        /**
         * Casts the lazy optional to the correct type
         */
        fun <X> cast(): LazyOptional<X> = lazy.cast()

        /**
         * Gets a result.
         *
         * @return a result
         */
        override fun get(): T = value

        /**
         * Returns the capability provided by a lazy optional
         */
        override fun invoke(): T = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun saveAdditional(pTag: CompoundTag) {
        super.saveAdditional(pTag)
        onSave(pTag)
        for (entry in handlers) {
            if (entry.value() is INBTSerializable<*>) {
                val handler = entry.value() as INBTSerializable<Tag>
                pTag.put(entry.key, handler.serializeNBT())
            }
        }
    }

    override fun onLoad() = this.onInit()

    @Suppress("UNCHECKED_CAST")
    override fun load(pTag: CompoundTag) {
        super.load(pTag)
        onLoad(pTag)
        for (entry in handlers) {
            if (entry.value() is INBTSerializable<*>) {
                val handler = entry.value() as INBTSerializable<Tag>
                handler.deserializeNBT(pTag.get(entry.key))
            }
        }
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(): CompoundTag {
        return serializeNBT()
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        for (entry in handlers) entry.value.lazy.invalidate()
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
}