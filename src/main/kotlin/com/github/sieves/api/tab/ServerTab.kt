package com.github.sieves.api.tab

import com.github.sieves.api.ApiTab
import com.github.sieves.util.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import com.github.sieves.registry.Registry.Net as Net

/**
 * This is used to render our tab
 */
@Suppress("UNCHECKED_CAST")
open class ServerTab(
    override val key: ResourceLocation, private val spec: TabSpec,
) : ApiTab {
    val uuid: UUID get() = getProperty("tab_data").get().getUUID("owner")
    protected var drawMenu = false
    protected val menuData = TabSpec.MenuData(0f, 0f, 0f, 0f)

    //Has to be internal for the Tab's equal check
    internal val nbt: MutableMap<String, CompoundTag> = HashMap()


    /**
     * Force our tab to tick when requested
     */
    override val isTicking: Boolean = spec.isClientTicking || spec.isServerTicking

    /**
     * The current tick of the api tab
     */
    override var tick: Int = 0

    /**
     * Called on the tick world last on the client
     */
    override fun tickClient(player: Player) {
        if (spec.isClientTicking) spec.clientTicker.get()(player, this)
    }

    /**
     * Called on the tick server event
     */
    override fun tickServer(serverPlayer: ServerPlayer) {
        if (spec.isServerTicking) spec.serverTicker.get()(serverPlayer, this)
    }

    /**
     * This is called upon the INBT Serialize event
     */
    override fun CompoundTag.serialize(): CompoundTag {
        putInt("properties", nbt.size)
        var index = 0
        for ((key, value) in nbt) {
            putString("key_$index", key)
            put("value_${index++}", value)
        }
        return this
    }

    /**
     * This is called upon the INBT Deserialize event
     */
    override fun CompoundTag.deserialize() {
        for (index in 0 until getInt("properties")) {
            try {
                val key = getString("key_$index")
                val value = getCompound("value_$index")
                nbt[key] = value
            } catch (ex: Exception) {
                Log.error { "Failed to deserialize tab: $key, error message: ${ex.message}" }
            }

        }
    }

    /**
     * This will display a notification (on client) for the given amount of [time].
     */
    override fun notify(image: ItemStack, component: Component, time: Float) {
        //TODO
    }

    /**
     * Gets the property with the given name
     */
    override fun getProperty(name: String): Optional<CompoundTag> = Optional.ofNullable(nbt[name])

    /**
     * Set a property of the given name. When simulate is true, the object won't be replaced,
     * but rather will have it's deserialize method called with the output of to serialize method
     * for the value
     */
    override fun setProperty(name: String, value: CompoundTag) {
        nbt[name] = value
    }

    /**
     * Removes the property of the given name
     */
    override fun removeProperty(name: String) {
        nbt.remove(name)
    }

    /**
     * Updates the server on the whereabouts of this tab
     */
    override fun syncToServer() {
        Net.sendToServer(Net.SyncTab {
            this.tab = this@ServerTab.serializeNBT()
            this.key = this@ServerTab.key
        })
    }

    /**
     * Syncs the stuffs to the client
     */
    override fun syncToClients() {
        Net.sendToAllClients(Net.SyncTab {
            this.tab = this@ServerTab.serializeNBT()
            this.key = this@ServerTab.key
        })
    }

    /**
     * Provide external access to the built propertiers
     */
    override fun getSpec(): TabSpec = spec

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServerTab) return false

        if (key != other.key) return false
        if (spec != other.spec) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + spec.hashCode()
        return result
    }

    override fun toString(): String {
        return "Tab(key=$key, properties=$spec, nbt=$nbt)"
    }


    companion object {

    }


}