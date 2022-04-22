package com.github.sieves.content.api.tab

import com.github.sieves.content.api.ApiTab
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import com.mojang.math.Vector4f
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.api.distmarker.Dist
import thedarkcolour.kotlinforforge.forge.runWhenOn
import java.util.*
import kotlin.collections.HashSet

/**
 * The tag specification is the immutable object that is built by the builder child class.
 */
data class TabSpec private constructor(
    val targets: Array<String>,
    val initializer: (tab: ApiTab) -> Unit,
    val clientClick: (player: Player, tab: ApiTab) -> Unit,
    val serverClick: (player: ServerPlayer, tab: ApiTab) -> Unit,
    val itemstackSupplier: (tab: ApiTab) -> ItemStack,
    val rotationSupplier: (tab: ApiTab) -> Quaternion,
    val tooltipSupplier: (tab: ApiTab) -> Component,
    val serverTicker: Optional<((player: ServerPlayer, tab: ApiTab) -> Unit)>,
    val clientTicker: Optional<((player: Player, tab: ApiTab) -> Unit)>,
    val drawMenu: (menuData: MenuData, player: Player, tab: ApiTab, container: Any) -> Unit,
    val isServerTicking: Boolean,
    val isClientTicking: Boolean,
    val drawItem: Boolean,
    val drawCount: Boolean,
    val drawToolTip: Boolean,
    val drawHover: Boolean,
    val drawSpin: Boolean,
    val drawLarger: Boolean,
    val hoverColor: Vector4f,
    val serializer: CompoundTag.(tab: ApiTab) -> CompoundTag,
    val deserializer: CompoundTag.(tab: ApiTab) -> Unit,
    val hasSerializer: Boolean,
    val hasDeserializer: Boolean,
    val hasInitializer: Boolean,
    val hasClientClick: Boolean,
    val hasServerClick: Boolean,
    val hasDrawMenu: Boolean
) {

    data class MenuData(var x: Float, var y: Float, var width: Float, var height: Float) {
        lateinit var poseStack: PoseStack
    }


    companion object {
        operator fun invoke(): Builder = Builder()
    }

    class Builder {
        private var itemstackSupplier: (tab: ApiTab) -> ItemStack = { ItemStack(Items.COAL_BLOCK) }
        private var initializer: (tab: ApiTab) -> Unit = {}
        private var clientClick: (player: Player, tab: ApiTab) -> Unit = { _, _ -> }
        private var serverClick: (player: ServerPlayer, tab: ApiTab) -> Unit = { _, _ -> }
        private var rotationSupplier: (tab: ApiTab) -> Quaternion = { Vector3f.YP.rotationDegrees(180f) }
        private var tooltipSupplier: (tab: ApiTab) -> Component = { itemstackSupplier(it).displayName }
        private var drawCount: Boolean = false
        private var drawToolTip: Boolean = false
        private var drawItem: Boolean = false
        private var drawHover: Boolean = false
        private var drawSpin: Boolean = false
        private var drawLarger: Boolean = false
        private var hoverColor: Vector4f = Vector4f(0.8f, 0.8f, 0.8f, 1f)
        private var targets: MutableSet<String> = HashSet()
        private var serverTicker: Optional<((player: ServerPlayer, tab: ApiTab) -> Unit)> = Optional.empty()
        private var clientTicker: Optional<((player: Player, tab: ApiTab) -> Unit)> = Optional.empty()
        private var drawMenu: (menuData: MenuData, player: Player, tab: ApiTab, container: Any) -> Unit =
            { _, _, _, _ -> }
        private var isServerTicking: Boolean = false
        private var isClientTicking: Boolean = false
        private var serializer: CompoundTag.(tab: ApiTab) -> CompoundTag = { this }
        private var deserializer: CompoundTag.(tab: ApiTab) -> Unit = { }
        private var hasSerializer: Boolean = false
        private var hasInitializer: Boolean = false
        private var hasDeserializer: Boolean = false
        private var hasClientClick: Boolean = false
        private var hasServerClick: Boolean = false
        private var hasDrawMenu: Boolean = false
        fun build(): TabSpec = TabSpec(
            targets.toTypedArray(),
            initializer,
            clientClick,
            serverClick,
            itemstackSupplier,
            rotationSupplier,
            tooltipSupplier,
            serverTicker,
            clientTicker,
            drawMenu,
            isServerTicking,
            isClientTicking,
            drawItem,
            drawCount,
            drawToolTip,
            drawHover,
            drawSpin,
            drawLarger,
            hoverColor,
            serializer,
            deserializer,
            hasSerializer,
            hasDeserializer,
            hasInitializer,
            hasClientClick,
            hasServerClick,
            hasDrawMenu
        )

        fun withMenu(drawMenu: (menuData: MenuData, player: Player, tab: ApiTab, container: Any) -> Unit): Builder {
            runWhenOn(Dist.CLIENT) {
                this.hasDrawMenu = true
                this.drawMenu = drawMenu
            }

            return this
        }

        fun withItem(draw: Boolean): Builder {
            this.drawItem = draw
            return this
        }

        fun withInit(initializer: (tab: ApiTab) -> Unit): Builder {
            hasInitializer = true
            this.initializer = initializer
            return this
        }

        fun withClientTick(ticker: (player: Player, tab: ApiTab) -> Unit): Builder {
            isClientTicking = true
            clientTicker = Optional.of(ticker)
            return this
        }

        fun withServerTick(ticker: (player: ServerPlayer, tab: ApiTab) -> Unit): Builder {
            isServerTicking = true
            serverTicker = Optional.of(ticker)
            return this
        }

        fun withClientClick(ticker: (player: Player, tab: ApiTab) -> Unit): Builder {
            clientClick = ticker
            return this
        }

        fun withServerClick(ticker: (player: ServerPlayer, tab: ApiTab) -> Unit): Builder {
            serverClick = ticker
            return this
        }

        fun withSerializer(serializer: CompoundTag.(tab: ApiTab) -> CompoundTag): Builder {
            hasSerializer = true
            this.serializer = serializer
            return this
        }

        fun withDeserializer(deserializer: CompoundTag.(tab: ApiTab) -> Unit): Builder {
            hasDeserializer = true
            this.deserializer = deserializer
            return this
        }

        fun withLargeBackground(): Builder {
            drawLarger = true
            return this
        }

        fun withItem(itemstackSupplier: (tab: ApiTab) -> ItemStack): Builder {
            this.itemstackSupplier = itemstackSupplier
            drawItem = true
            return this
        }

        fun withRotation(rotationSupplier: (tab: ApiTab) -> Quaternion): Builder {
            this.rotationSupplier = rotationSupplier
            return this
        }

        fun withTooltip(tooltipSupplier: (tab: ApiTab) -> Component): Builder {
            this.tooltipSupplier = tooltipSupplier
            this.drawToolTip = true
            return this
        }

        fun withTooltip(): Builder {
            drawToolTip = true
            return this
        }


        fun withCount(): Builder {
            this.drawCount = true
            return this
        }

        fun withToolTip(): Builder {
            this.drawToolTip = true
            return this
        }

        fun withSpin(): Builder {
            this.drawSpin = true
            return this
        }

        fun withHover(color: Vector4f): Builder {
            this.hoverColor = color
            drawHover = true
            return this
        }

        fun withHover(color: String): Builder {
            this.hoverColor = if (color.length > 8) Vector4f(
                Integer.valueOf(color.substring(1, 3), 16) / 255f,
                Integer.valueOf(color.substring(3, 5), 16) / 255f,
                Integer.valueOf(color.substring(5, 7), 16) / 255f,
                Integer.valueOf(color.substring(7, 9), 16) / 255f,
            )
            else Vector4f(
                Integer.valueOf(color.substring(1, 3), 16) / 255f,
                Integer.valueOf(color.substring(3, 5), 16) / 255f,
                Integer.valueOf(color.substring(5, 7), 16) / 255f,
                1.0f
            )
            drawHover = true
            return this
        }

        fun withHover(): Builder {
            drawHover = true
            return this
        }

        @Suppress("UNCHECKED_CAST")
        fun withTarget(clazz: String): Builder {
            this.targets.add(clazz)
            return this
        }

//        @Suppress("UNCHECKED_CAST")
//        fun <T : AbstractContainerScreen<*>> withTarget(clazz: Class<T>): Builder {
//            this.targets.add(clazz.name)
//            return this
//        }
//
//        inline fun <reified T : AbstractContainerScreen<*>> withTarget(): Builder = withTarget(T::class.java)

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TabSpec) return false

        if (!targets.contentEquals(other.targets)) return false
        if (itemstackSupplier != other.itemstackSupplier) return false
        if (rotationSupplier != other.rotationSupplier) return false
        if (tooltipSupplier != other.tooltipSupplier) return false
        if (drawCount != other.drawCount) return false
        if (drawToolTip != other.drawToolTip) return false
        if (drawHover != other.drawHover) return false
        if (drawSpin != other.drawSpin) return false
        if (hoverColor != other.hoverColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = targets.contentHashCode()
        result = 31 * result + itemstackSupplier.hashCode()
        result = 31 * result + rotationSupplier.hashCode()
        result = 31 * result + tooltipSupplier.hashCode()
        result = 31 * result + drawCount.hashCode()
        result = 31 * result + drawToolTip.hashCode()
        result = 31 * result + drawHover.hashCode()
        result = 31 * result + drawSpin.hashCode()
        result = 31 * result + hoverColor.hashCode()
        return result
    }
}