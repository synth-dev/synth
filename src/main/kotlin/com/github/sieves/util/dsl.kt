package com.github.sieves.util

import com.github.sieves.Sieves
import com.github.sieves.api.ApiTab
import com.github.sieves.api.ApiTabItem
import com.github.sieves.api.tab.TabRegistry
import com.github.sieves.api.tab.TabSpec
import com.github.sieves.registry.internal.IRegister
import com.github.sieves.registry.internal.Registry
import com.github.sieves.registry.internal.net.Packet
import com.github.sieves.util.Log.debug
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.util.LogicalSidedProvider
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.util.thread.SidedThreadGroups
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.items.wrapper.InvWrapper
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.server.ServerLifecycleHooks
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.sqrt
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf

/**
 * ==========================WARNING===========================
 *
 *                            _             _ _ _ _ _
 *  __      ____ _ _ __ _ __ (_)_ __   __ _| | | | | |
 *  \ \ /\ / / _` | '__| '_ \| | '_ \ / _` | | | | | |
 *   \ V  V / (_| | |  | | | | | | | | (_| |_|_|_|_|_|
 *    \_/\_/ \__,_|_|  |_| |_|_|_| |_|\__, (_|_|_|_|_)
 *    |___/
=============================WARNING===========================

This is where I hide all of my dirty little kotlin secrets..
Do not read this kts, you will not! like what you find...
you've been warned...
 */


object Log {
    val logger = LogManager.getLogger(Sieves.ModId)

    inline fun info(supplier: () -> String) = logger.info(supplier())
    inline fun warn(supplier: () -> String) = logger.warn(supplier())
    inline fun debug(supplier: () -> String) = logger.debug(supplier())
    inline fun error(supplier: () -> String) = logger.error(supplier())
    inline fun trace(supplier: () -> String) = logger.trace(supplier())
}

enum class NetDir {
    ToClient, ToServer, Other
}

fun Packet.getLevel(key: ResourceKey<Level>): Level {
    return ServerLifecycleHooks.getCurrentServer().getLevel(key)!!
}


/**
 * Convert a generic map to nbt
 */
inline fun <reified K : Any, reified V : Any> Map<K, V>.toCompound(): CompoundTag {
    val tag = CompoundTag()
    tag.putInt("size", this.size)
    val store = CompoundTag()
    this.keys.forEachIndexed { i, key ->
        store.tryPut("key_$i", key)
        store.tryPut("value_$i", this[key]!!)
    }
    tag.put("store", store)
    return tag
}

/**
 * Deserialize the map
 */
inline fun <reified K : Any, reified V : Any> CompoundTag.toMap(): MutableMap<K, V> {
    val map = HashMap<K, V>()
    val size = this.getInt("size")
    val store = this.getCompound("store")
    for (i in 0 until size) {
        val key = store.tryGet<K>("key_$i")
        val value = store.tryGet<V>("value_$i")
        if (key != null && value != null) map[key] = value
    }
    return map
}

operator fun CompoundTag.invoke(name: String): CompoundTag = this.getCompound(name)


/**
 * Convert a generic map to nbt
 */
inline fun <reified V : Any> List<V>.toCompound(): CompoundTag {
    val tag = CompoundTag()
    tag.putInt("size", this.size)
    val store = CompoundTag()
    this.forEachIndexed { i, key ->
        store.tryPut("value_$i", key)
    }
    tag.put("store", store)
    return tag
}

/**
 * Deserialize the map
 */
inline fun <reified V : Any> CompoundTag.toList(): MutableList<V> {
    val list = ArrayList<V>()
    val size = this.getInt("size")
    val store = this.getCompound("store")
    for (i in 0 until size) {
        val value = store.tryGet<V>("value_$i")
        if (value != null) list.add(value)
    }
    return list
}

/**
 * Attempts to serialize a lot of stufff
 */
fun <T : Any> CompoundTag.tryPut(name: String, value: T): Boolean {
    when (value) {
        is BlockPos -> this.putBlockPos(name, value)
        is Int -> this.putInt(name, value)
        is Float -> this.putFloat(name, value)
        is Double -> this.putDouble(name, value)
        is Boolean -> this.putBoolean(name, value)
        is Byte -> this.putByte(name, value)
        is String -> this.putString(name, value)
        is ItemStack -> this.put(name, value.serializeNBT())
        is ResourceLocation -> {
            val subTag = CompoundTag()
            subTag.putString("namespace", value.namespace)
            subTag.putString("path", value.path)
            this.put(name, subTag)
        }
        is Block -> return this.tryPut(name, value.registryName!!)
        is UUID -> this.putUUID(name, value)
        is IntArray -> this.putIntArray(name, value)
        is ByteArray -> this.putByteArray(name, value)
        is LongArray -> this.putLongArray(name, value)
        is CompoundTag -> this.put(name, value)
        else -> {
            Log.warn { "Attempted to serialize unknown serialization type of ${value::class.qualifiedName} for name $name" }
            return false
        }
    }
//    debug { "Serialized ${value::class.simpleName} with name: $name and value: $value" }
    return true
}

inline fun <reified T : Any> CompoundTag.tryGet(name: String): T? = tryGet(name, T::class)

/**
 * Attempts to get the value
 */
fun <T : Any> CompoundTag.tryGet(name: String, classType: KClass<T>): T? {
    return when (classType) {
        BlockPos::class -> classType.cast(this.getBlockPos(name))
        Int::class -> classType.cast(this.getInt(name))
        Float::class -> classType.cast(this.getFloat(name))
        Double::class -> classType.cast(this.getDouble(name))
        Boolean::class -> classType.cast(this.getBoolean(name))
        Byte::class -> classType.cast(this.getByte(name))
        String::class -> classType.cast(this.getString(name))
        UUID::class -> classType.cast(this.getUUID(name))
        ItemStack::class -> classType.cast(ItemStack.of(getCompound(name)))
        IntArray::class -> classType.cast(this.getIntArray(name))
        ByteArray::class -> classType.cast(this.getByteArray(name))
        LongArray::class -> classType.cast(this.getLongArray(name))
        CompoundTag::class -> classType.cast(this.getCompound(name))
        Block::class -> {
            var result: T? = null
            this.tryGet(name, ResourceLocation::class)?.let {
                val block = net.minecraft.core.Registry.BLOCK.get(it)
                result = classType.cast(block)
            }
            result
        }
        ResourceLocation::class -> {
            val subTag = this.getCompound(name)
            return classType.cast(ResourceLocation(subTag.getString("namespace"), subTag.getString("path")))
        }
        else -> null
    }
}


/**
 * Gets the player based on the uuid, ONLY WORKS ON SERVER
 */
val UUID.asPlayer: Optional<ServerPlayer>
    get() = Optional.ofNullable((ServerLifecycleHooks.getCurrentServer().playerList.getPlayer(this)))

fun ApiTabItem.Companion.removeItem(player: Player, tab: ApiTab, tag: CompoundTag? = null) {
    val stack = tab.getSpec().itemstackSupplier(tab)
    TabRegistry.unbindTab(player.uuid, tab.key)
    if (tag != null) stack.tag = tag
    val inserted = ItemHandlerHelper.insertItem(InvWrapper(player.inventory), stack, false)
    if (!inserted.isEmpty) {
        player.level.addFreshEntity(ItemEntity(player.level, player.x, player.y, player.z, inserted))
    }
}

val NetworkEvent.Context.dir: NetDir
    get() {
        return when (direction) {
            NetworkDirection.PLAY_TO_SERVER -> NetDir.ToServer
            NetworkDirection.PLAY_TO_CLIENT -> NetDir.ToClient
            NetworkDirection.LOGIN_TO_SERVER -> NetDir.Other
            NetworkDirection.LOGIN_TO_CLIENT -> NetDir.Other
        }
    }

fun AbstractContainerScreen<*>.isHovered(
    pX: Int, pY: Int, pWidth: Int, pHeight: Int, mouseX: Double, mouseY: Double,
): Boolean {
    val pMouseX = mouseX - this.guiLeft
    val pMouseY = mouseY - this.guiTop
    return pMouseX >= (pX - 1).toDouble() && pMouseX < (pX + pWidth + 1).toDouble() && pMouseY >= (pY - 1).toDouble() && pMouseY < (pY + pHeight + 1).toDouble()
}

private var lastClick = System.currentTimeMillis()

fun AbstractContainerScreen<*>.isClicked(
    x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double,
): Boolean {
    val now = System.currentTimeMillis()
    if (isHovered(x, y, width, height, mouseX, mouseY) && GLFW.glfwGetMouseButton(
            Minecraft.getInstance().window.window,
            GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS && now - lastClick > 250
    ) {
        lastClick = now
        return true
    }
    return false
}

fun AbstractContainerScreen<*>.isRightClicked(
    x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double,
): Boolean {
    val now = System.currentTimeMillis()
    if (isHovered(x, y, width, height, mouseX, mouseY) && GLFW.glfwGetMouseButton(
            Minecraft.getInstance().window.window,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT
        ) == GLFW.GLFW_PRESS && now - lastClick > 250
    ) {
        lastClick = now
        return true
    }
    return false
}

fun AbstractContainerScreen<*>.drawText(
    stack: PoseStack,
    x: Float, y: Float, text: String, color: Int,
) = Minecraft.getInstance().font.draw(stack, text, x, y, color)

fun AbstractContainerScreen<*>.drawText(
    stack: TabSpec.MenuData,
    x: Float, y: Float, text: String, color: Int,
) = Minecraft.getInstance().font.draw(stack.poseStack, text, x, y, color)


fun AbstractContainerScreen<*>.drawTextShadow(
    stack: PoseStack,
    x: Float, y: Float, text: String, color: Int,
) = Minecraft.getInstance().font.drawShadow(stack, text, x, y, color)

fun AbstractContainerScreen<*>.drawTextShadow(
    stack: TabSpec.MenuData,
    x: Float, y: Float, text: String, color: Int,
) = Minecraft.getInstance().font.drawShadow(stack.poseStack, text, x, y, color)


/**
 * Gets a resource location based upon the give string
 */
internal val String.resLoc: ResourceLocation
    get() = ResourceLocation(Sieves.ModId, this)


/**
 * This is used for easy block entity registration
 */
inline fun <reified T : BlockEntity> Registry<BlockEntityType<*>>.tile(
    block: Block, crossinline supplier: (Pair<BlockPos, BlockState>) -> T,
): BlockEntityType<T> {
    return BlockEntityType.Builder.of({ pos, state -> supplier(pos to state) }, block).build(null)
}

inline fun <reified T : IRegister> T.registerAll(
    modID: String = Sieves.ModId, modBus: IEventBus = MOD_BUS, forgeBus: IEventBus = FORGE_BUS,
) {
    for (child in this::class.nestedClasses) {
        if (child.isSubclassOf(IRegister::class)) {
            val instance = child.objectInstance ?: continue
            if (instance is IRegister) {
                instance.register(modID, modBus, forgeBus)
                Log.info { "Successfully registered the '${child.simpleName}' registry" }
            }
        }
    }
}

fun <E : Enum<E>> CompoundTag.putEnum(name: String, enum: E): CompoundTag {
    this.putInt("${name}_enum", enum.ordinal)
    return this
}

/**
 * Gets the enum of the given name
 */
inline fun <reified E : Enum<E>> CompoundTag.getEnum(name: String): E {
    val contents = E::class.java.enumConstants
    val value = this.getInt("${name}_enum")
    return contents[value]
}


fun BlockPos.getInflatedAAABB(inflate: Float): AABB {
    return AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(this)).inflate(inflate.toDouble())
}

//inline fun BlockPos.forEachBlockIn(radius: Int, iterator: (BlockPos) -> Unit) {
//    val aabb = this.getInflatedAAABB(radius.toFloat())
//}


/**
 * This will raytrace the given distance for the given player
 */
fun Player.rayTrace(distance: Double = 75.0): BlockHitResult {
    val rayTraceResult = pick(distance, 0f, false) as BlockHitResult
    var xm = rayTraceResult.location.x
    var ym = rayTraceResult.location.y
    var zm = rayTraceResult.location.z
    var pos = BlockPos(xm, ym, zm)
    val block = level.getBlockState(pos)
    if (block.isAir) {
        if (rayTraceResult.direction == Direction.SOUTH) zm--
        if (rayTraceResult.direction == Direction.EAST) xm--
        if (rayTraceResult.direction == Direction.UP) ym--
    }
    pos = BlockPos(xm, ym, zm)
    return BlockHitResult(rayTraceResult.location, rayTraceResult.direction, pos, false)
}

fun VoxelShape.join(other: VoxelShape, op: BooleanOp): VoxelShape {
    return Shapes.join(this, other, op)
}

fun CompoundTag.putBlockPos(name: String, blockPos: BlockPos) {
    putIntArray(name, intArrayOf(blockPos.x, blockPos.y, blockPos.z))
}

fun CompoundTag.getBlockPos(name: String): BlockPos {
    val array = getIntArray(name)
    if (array.size != 3) return BlockPos.ZERO
    return BlockPos(array[0], array[1], array[2])
}

fun Vector3f.length(scale: Float = 1.0f): Float {
    val dist = sqrt(x() * x() + (y() * y()) + (z() + z()).toDouble())
    return (dist * scale).toFloat()
}


/**This boolean checks to see if the current program is on the physical client or not**/
internal val physicalClient: Boolean
    get() = FMLEnvironment.dist == Dist.CLIENT

/**This boolean checks to see if the current program is on the physical server or not**/
internal val physicalServer: Boolean
    get() = FMLEnvironment.dist == Dist.DEDICATED_SERVER

/**This boolean checks to see if the current thread group is thew logical client**/
internal val logicalClient: Boolean
    get() {
        if (physicalServer) return false //This is so we don't end up calling [Minecraft] calls from the client
        if (Thread.currentThread().threadGroup == SidedThreadGroups.CLIENT) return true
        try {
            if (RenderSystem.isOnRenderThread()) return true
        } catch (notFound: ClassNotFoundException) {
            return false //We're not on the client if there's a class not found execetion
        }
        return false
    }


/**This boolean checks to see if the current thread group is thew logical client**/
internal val logicalServer: Boolean
    get() = Thread.currentThread().threadGroup == SidedThreadGroups.SERVER

/**This boolean checks to see if the current thread group is thew logical client**/
internal val logicalRender: Boolean
    get() = RenderSystem.isOnRenderThread()

/**
 * This block of code will execute only if we're on the physical client
 */
internal fun whenClient(logical: Boolean = true, block: () -> Unit) {
    if (logical && logicalClient) block()
    else if (!logical && physicalClient) block()
}

/**
 * This block of code will execute only if we're on the physical client
 */
internal fun whenServer(logical: Boolean = true, block: () -> Unit) {
    if (logical && logicalServer) block()
    else if (!logical && physicalServer) block()
}

/**
 * This will run the given block on the logical side
 */
internal fun runOn(side: LogicalSide, block: () -> Unit): CompletableFuture<Void> {
    val executor = LogicalSidedProvider.WORKQUEUE.get(side)
    return if (!executor.isSameThread) executor.submit(block) // Use the internal method so thread check isn't done twice
    else {
        block()
        CompletableFuture.completedFuture(null)
    }
}

/**
 * Wraps the operator around the slot handler
 */
internal operator fun IItemHandler.get(slot: Int): ItemStack = getStackInSlot(slot)

/**
 * This run the given chunk of code on the client
 */
internal fun runOnClient(block: () -> Unit): CompletableFuture<Void> {
    return runOn(LogicalSide.CLIENT, block)
}

/**
 * This will run the render method
 */
internal fun runOnRender(block: () -> Unit) {
    if (logicalRender) block()
    else RenderSystem.recordRenderCall(block)
}

/**
 * This run the given chunk of code on the server
 */
internal fun runOnServer(block: () -> Unit): CompletableFuture<Void> {
    return runOn(LogicalSide.SERVER, block)
}
