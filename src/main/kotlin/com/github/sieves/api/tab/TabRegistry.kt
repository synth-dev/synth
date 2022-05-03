package com.github.sieves.api.tab

import com.github.sieves.api.ApiTab
import com.github.sieves.registry.Registry
import com.github.sieves.registry.internal.ListenerRegistry
import com.github.sieves.registry.internal.net.*
import com.github.sieves.dsl.*
import com.github.sieves.dsl.Log.debug
import com.github.sieves.dsl.Log.error
import com.github.sieves.dsl.Log.info
import com.github.sieves.dsl.logicalServer
import com.github.sieves.dsl.physicalClient
import net.minecraft.client.Minecraft

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.Dist.*
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.client.event.ScreenOpenEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStoppingEvent
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
    private val containerRegistry: MutableMap<String, MutableSet<ResourceLocation>> = HashMap()
    private val tabRegistry: MutableMap<ResourceLocation, ApiTab> = HashMap()
    private val tabFactories: MutableMap<ResourceLocation, TabFactory> = HashMap()
    internal val activeTabs: MutableMap<UUID, ConcurrentHashMap<ResourceLocation, ApiTab>> = ConcurrentHashMap()
    private val openedMenus: MutableMap<UUID, ResourceLocation> = HashMap()
    private const val tickTarget = 20 //Every half a second the client and server is
    private var serverTickTime = 0

    /**
     * Regsters all the shitz
     */
    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        info { "Registering the tab registry..." }
        Registry.Net.SyncTab.serverListener(::onSyncServer)
        Registry.Net.SyncTab.clientListener(::onSyncClient)
        Registry.Net.ClickTab.serverListener(::onClick)
        Registry.Net.MenuOpen.serverListener(::onOpenMenu)

        forgeBus.addListener(::onClientTick)
        forgeBus.addListener(::onServerTick)
        forgeBus.addListener(::onLevelSaved)
        forgeBus.addListener(::onLevelLoaded)
        forgeBus.addListener(::onPlayerLeave)
        runWhenOn(Dist.CLIENT) {
            forgeBus.addListener(TabRegistry::renderOverlay)
            forgeBus.addListener(TabRegistry::onScreenOpen)
        }
    }

    private fun onPlayerLeave(event: ServerStoppingEvent) {
        activeTabs.clear() //attempt to fix for
        runWhenOn(CLIENT) {
            Minecraft.getInstance().options.gamma = 0.0
        }
    }

    /**
     * Load our persisted data for each world upon the world loading
     */
    private fun onLevelSaved(worldEvent: WorldEvent.Load) {
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
                        debug { "Loaded tab: $tab" }
                    }
                }
            }
        }
    }


    /**
     * Make out tab data persist, it is mainly stored on the overworld as its the only world to not be unloaded
     */
    private fun onLevelLoaded(worldEvent: WorldEvent.Save) {
        val level = worldEvent.world
        if (level.isClientSide) return
        if (level is ServerLevel) {
            val data = TabData[level]
            for (tab in activeTabs) {
                val map = ConcurrentHashMap<ResourceLocation, CompoundTag>()
                for (value in tab.value) map[value.key] = value.value.serializeNBT()
                data.tabs[tab.key] = map
                debug { "Saved tab: $tab" }
            }
        }
    }

    /**
     * Called on the server upon clicking a button
     */
    private fun onOpenMenu(openMenuPacket: MenuStatePacket, context: Context): Boolean {
        openedMenus[openMenuPacket.player] = openMenuPacket.menu
        debug { "Got menu open packet: $openMenuPacket" }
        return true
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
        info { "registered factory: $resourceLocation" }
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
        whenServer {
            val tabData = TabData[ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD)!!]
            for (curTab in activeTabs) {
                val map = ConcurrentHashMap<ResourceLocation, CompoundTag>()
                for (value in curTab.value) map[value.key] = value.value.serializeNBT()
                tabData.tabs[curTab.key] = map
            }
            tabData.setDirty()
        }
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
     * Renders all of our tool tips and overlayed stuff
     */
    @OnlyIn(Dist.CLIENT)
    private fun renderOverlay(event: ScreenEvent.DrawScreenEvent.Post) {
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
                if ((y) < screen.ySize - 20) {
                    if (containerRegistry[screen::class.java.name]?.contains(group.key) == true)
                        y += (group as Tab).preRender(
                            event.poseStack,
                            x.toFloat(),
                            y.toFloat(),
                            event.mouseX.toDouble(),
                            event.mouseY.toDouble(),
                            screen
                        )
                    y += 3
                }
            }
            y = 3
            for (group in activeTabs[Minecraft.getInstance().player?.uuid!!]!!.values) {
                if (containerRegistry[screen::class.java.name]?.contains(group.key) == true) y += (group as Tab).postRender(
                    event.poseStack, x.toFloat(), y.toFloat(), event.mouseX.toDouble(), event.mouseY.toDouble(), screen
                )
                y += 3
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
     * Attempts to get the currently open tab for the player
     */
    fun getOpenMenu(uuid: UUID): Optional<ResourceLocation> {
        return Optional.ofNullable(openedMenus[uuid])
    }

    /**
     * This will set the current menu to be this menu
     */
    fun setOpenMenu(tab: Tab) {
        openedMenus[tab.uuid] = tab.key
    }

    fun removeOpenMenu(uuid: UUID): Boolean {
        return openedMenus.remove(uuid) != null
    }

    /**
     * Call all the ticking tab's tick methods
     */
    private fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (!physicalClient) return
        val player = Minecraft.getInstance().player ?: return
        val iterator = this.activeTabs[player.uuid] ?: return
        for (tab in iterator.values) {
            tab.tick++
            if (tab.getSpec().isClientTicking && tab.tick >= tab.getSpec().interval) {
                tab.tickClient(player)
                tab.tick = 0
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun onScreenOpen(event: ScreenOpenEvent) = runWhenOn(Dist.CLIENT) {
        if (event.screen == null) {
            //Clear open menus locally
            openedMenus.remove(Minecraft.getInstance().player!!.uuid)
        }
    }

    /**
     * This is where the core of the tab logic happens. Here we tick all the active tabs for each player, every tick.
     */
    private fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (!logicalServer) return
        for (group in this.activeTabs) {
            val player = group.key.asPlayer
            if (player.isEmpty) continue
            for (tab in group.value.values) {
                tab.tick++
                if (tab.getSpec().isServerTicking && tab.tick >= tab.getSpec().interval) {
                    tab.tickServer(player.get())
                    tab.tick = 0
                }
            }
        }
    }

    /**
     * Gets a ApiTab from the defined tab's registry via it's key
     */
    operator fun get(resourceLocation: ResourceLocation): Optional<ApiTab> {
        return Optional.ofNullable(tabRegistry[resourceLocation])
    }


}

