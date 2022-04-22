package com.github.sieves.content.api.tab

import com.github.sieves.content.api.ApiTab
import com.github.sieves.registry.Registry
import com.github.sieves.registry.internal.ListenerRegistry
import com.github.sieves.registry.internal.net.TabBindPacket
import com.github.sieves.registry.internal.net.TabClickedPacket
import com.github.sieves.registry.internal.net.TabUpdatePacket
import com.github.sieves.util.*
import com.github.sieves.util.Log.debug
import com.github.sieves.util.Log.error
import com.github.sieves.util.Log.info
import com.github.sieves.util.logicalServer
import com.github.sieves.util.physicalClient
import net.minecraft.client.Minecraft

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.dimension.DimensionType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.network.NetworkEvent.Context
import net.minecraftforge.server.ServerLifecycleHooks
import thedarkcolour.kotlinforforge.forge.runWhenOn
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.collections.HashSet


object TabRegistry : ListenerRegistry() {
    private val containerRegistry: MutableMap<String, MutableSet<ResourceLocation>> =
        HashMap()
    private val tabRegistry: MutableMap<ResourceLocation, ApiTab> = HashMap()
    private val tabFactories: MutableMap<ResourceLocation, TabFactory> = HashMap()
    internal val activeTabs: MutableMap<UUID, ConcurrentHashMap<ResourceLocation, ApiTab>> = ConcurrentHashMap()
    private const val tickTarget = 20 //Every half a second the client and server is
    private var serverTickTime = 0
    private var clientTickTime = 0

    /**
     * Regsters all the shitz
     */
    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        info { "Registering the tab registry..." }
        Registry.Net.SyncTab.serverListener(::onSyncServer)
        Registry.Net.SyncTab.clientListener(::onSyncClient)
        Registry.Net.BindTab.clientListener(::onBind)
        Registry.Net.BindTab.serverListener(::onBind)
        Registry.Net.ClickTab.serverListener(::onClick)
        runWhenOn(Dist.CLIENT) {
            forgeBus.addListener(TabRegistry::preRender)
            forgeBus.addListener(TabRegistry::postRender)
        }
        forgeBus.addListener(::onClientTick)
        forgeBus.addListener(::onServerTick)
        forgeBus.addListener(::onLevelLoad)
        forgeBus.addListener(::onLevelSave)

    }

    private fun onLevelLoad(worldEvent: WorldEvent.Load) {
        val level = worldEvent.world
        if (level.isClientSide) return
        if (level is ServerLevel) {
            val data = TabData[level]
            data.tabs.forEach { (t, u) ->
                u.forEach { (key, tag) ->
                    val tab = createTab(t, key)
                    tab.ifPresent {
                        it.deserializeNBT(tag)
                        bindTab(it)
                    }
                }
            }
        }
    }


    private fun onLevelSave(worldEvent: WorldEvent.Save) {
        val level = worldEvent.world
        if (level.isClientSide) return
        if (level is ServerLevel) {
            val data = TabData[level]

            for (tab in activeTabs) {
                val map = ConcurrentHashMap<ResourceLocation, CompoundTag>()
                for (value in tab.value) {
                    map[value.key] = value.value.serializeNBT()
                }
                data.tabs[tab.key] = map
            }
        }
    }

    /**
     * Called on the server upon clicking a button
     */
    private fun onClick(tabClickedPacket: TabClickedPacket, context: Context): Boolean {
        val tabOpt = getBoundTab(tabClickedPacket.uuid, tabClickedPacket.key)
        if (tabOpt.isEmpty) return false
        val tab = tabOpt.get()
        val player = tabClickedPacket.uuid.asPlayer
        player.ifPresent {
            tab.getSpec().serverClick(it, tab)
        }
        return true
    }

    /**
     * Gets the tab of the given resource type that is bounds to the player with the given uuid
     */
    fun getBoundTab(uuid: UUID, resourceLocation: ResourceLocation): Optional<ApiTab> {
        return Optional.ofNullable(activeTabs[uuid]?.get(resourceLocation))
    }


    /**
     * Registers the tab using the appropriate properties
     */
    fun registerTab(tab: ApiTab) {
        tabRegistry[tab.key] = tab
        tab.getSpec().targets.forEach {
            containerRegistry.getOrPut(it) { HashSet() }.add(tab.key)
        }
        info { "Registered tab with key ${tab.key}, and properties ${tab.getSpec()}" }
    }

    /**
     * Registers the tab provider
     */
    fun registerTabFactory(resourceLocation: ResourceLocation, tabFactory: TabFactory) {
        tabFactories[resourceLocation] = tabFactory
    }

    /**
     * Checks to see if the the player has the given tab
     */
    fun hasTab(uuid: UUID, resourceLocation: ResourceLocation): Boolean {
        if (!activeTabs.containsKey(uuid)) return false
        val tabs = activeTabs[uuid]!!
        for (tab in tabs) {
            if (tab.key == resourceLocation) return true
        }
        return false
    }

    /**
     * Unbinds the tab from the player
     */
    fun unbindTab(uuid: UUID, resourceLocation: ResourceLocation) {
        activeTabs[uuid]?.remove(resourceLocation)
    }

    /**
     * Creates a tag from the factory for given player
     */
    fun createTab(uuid: UUID, resourceLocation: ResourceLocation): Optional<ApiTab> {
        if (!tabFactories.containsKey(resourceLocation)) {
            error { "Attempting to bind to unregistered factory of tab: $resourceLocation" }
            return Optional.empty()
        }
        val factory = tabFactories[resourceLocation] ?: error("This really should not happen ever")
        val tab = factory.create(uuid)
        if (tab.getSpec().hasInitializer) {
            tab.getSpec().initializer(tab)
        }
        return Optional.of(tab)
    }

    /**
     * This is the magical method that will bind a given tab to a player.
     * This will likely be done with capabilities in the future? or some other means
     * when push update is true, we will update the server or client depending on the [direction]
     * return true if the tab was bound, false if it's already bound
     */
    fun bindTab(
        tab: ApiTab
    ): Boolean {
        val data = tab.getProperty("tab_data")
        if (data.isEmpty) return false
        val uuid = data.get().getUUID("owner")
        val target = activeTabs.getOrPut(uuid) { ConcurrentHashMap() }
        if (target.containsKey(tab.key)) return false
        target[tab.key] = tab
        val tabData = TabData[ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD)!!]
        for (curTab in activeTabs) {
            val map = ConcurrentHashMap<ResourceLocation, CompoundTag>()
            for (value in curTab.value) {
                map[value.key] = value.value.serializeNBT()
            }
            tabData.tabs[curTab.key] = map
        }
        tabData.setDirty()
        return true
    }

    /**
     * This will create and bind the given tab, also allowing you to configure it
     * returns true if created/configured successfully
     */
    fun createAndBind(uuid: UUID, resourceLocation: ResourceLocation, configure: (ApiTab) -> Unit = {}): Boolean {
        val tab = createTab(uuid, resourceLocation)
        if (tab.isEmpty) return false
        configure(tab.get())
        if (!bindTab(tab.get())) return false
        return true
    }

    /**
     * Sync our bindings but don't push the update
     */
    private fun onBind(packet: TabBindPacket, context: Context): Boolean {
//        bindTab(packet.uuid, packet.key, context.dir, false)
        return true
    }

    /**
     * This will return true if our local player is bound/active for the current player.
     * this only works on the client
     */
    private fun isLocalPlayerBound(resourceLocation: ResourceLocation): Boolean {
        if (!physicalClient) return false
        val uuid = Minecraft.getInstance().player?.uuid ?: return false
        val active = activeTabs[uuid] ?: return false
        for (tab in active) {
            if (tab.key == resourceLocation) return true
        }
        return false
    }

    /**
     * rends all of our rendering stuff
     */
    private fun preRender(event: ScreenEvent.DrawScreenEvent.Pre) {
//        val screen = event.screen
//        if (screen is AbstractContainerScreen<*>) {
//            val x = 19
//            var y = 0
//            val uuid = Minecraft.getInstance().player?.uuid ?: return
//            if (!activeTabs.containsKey(uuid)) return
//            for (group in activeTabs[Minecraft.getInstance().player?.uuid!!]!!) {
//                group.preRender(
//                    event.poseStack, x.toFloat(), y.toFloat(), event.mouseX.toDouble(), event.mouseY.toDouble(), screen
//                )
//                y += 25
//            }
//        }
    }

    /**
     * Renders all of our tool tips and overlayed stuff
     */
    @OnlyIn(Dist.CLIENT)
    private fun postRender(event: ScreenEvent.DrawScreenEvent.Post) {
        val screen = event.screen
        if (screen is net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*> && containerRegistry.containsKey(
                screen::class.java.name
            )
        ) {
            val x = 19
            var y = 5
            val uuid = Minecraft.getInstance().player?.uuid ?: return
            if (!activeTabs.containsKey(uuid)) return
            for (group in activeTabs[Minecraft.getInstance().player?.uuid!!]!!.values) {
                if (containerRegistry[screen::class.java.name]?.contains(group.key) == true)

                    y += (group as Tab).preRender(
                        event.poseStack,
                        x.toFloat(),
                        y.toFloat(),
                        event.mouseX.toDouble(),
                        event.mouseY.toDouble(),
                        screen
                    )
                y += 5
            }
            y = 5
            for (group in activeTabs[Minecraft.getInstance().player?.uuid!!]!!.values) {
                if (containerRegistry[screen::class.java.name]?.contains(group.key) == true)
                    y += (group as Tab).postRender(
                        event.poseStack,
                        x.toFloat(),
                        y.toFloat(),
                        event.mouseX.toDouble(),
                        event.mouseY.toDouble(),
                        screen
                    )
                y += 5
            }
        }
    }

    /**
     * Updates the tab on the client from the server
     */
    private fun onSyncClient(packet: TabUpdatePacket, context: Context): Boolean {
        val tab = get(packet.key)
        if (tab.isEmpty) return false
        tab.get().deserializeNBT(packet.tab)
        info { "Finished syncing client with key ${packet.key}, properties: ${tab.get().getSpec()}" }
        return true
    }

    /**
     * Updates the tab on the client from the server
     */
    private fun onSyncServer(packet: TabUpdatePacket, context: Context): Boolean {
        val tab = get(packet.key)
        if (tab.isEmpty) return false
        tab.get().deserializeNBT(packet.tab)
        info { "Finished syncing server with key ${packet.key}, properties: ${tab.get().getSpec()}" }
        return true
    }

    /**
     * Call all the ticking tab's tick methods
     */
    private fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (!physicalClient) return
        for (tab in this.activeTabs.values) {
            tab.values.forEach {
                val player = Minecraft.getInstance().player ?: return@forEach
                if (it.getSpec().isClientTicking) it.tickClient(player)
            }
        }
    }

    /**
     * Call all the ticking tab's tick methods
     */

    private fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (!logicalServer) return
        serverTickTime++
        if (serverTickTime > 5) {
            serverTickTime = 0
            for (tab in this.activeTabs) {
                val player = tab.key.asPlayer
                if (player.isEmpty) continue
                tab.value.forEach {
                    if (it.value.getSpec().isServerTicking) it.value.tickServer(player.get())
                }
            }
        }
    }

    /**
     * Get the playerlist from the server hook
     */
    private fun players(): List<ServerPlayer> = ServerLifecycleHooks.getCurrentServer().playerList.players


    operator fun get(resourceLocation: ResourceLocation): Optional<ApiTab> {
        return Optional.ofNullable(tabRegistry[resourceLocation])
    }


}

