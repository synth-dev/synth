package com.github.sieves.registry

import com.github.sieves.recipes.SieveRecipe
import com.github.sieves.recipes.SieveRecipe.Serializer
import com.github.sieves.util.resLoc
import com.github.sieves.util.tile
import com.github.sieves.compat.top.TopPlugin
import com.github.sieves.content.api.tab.Tab
import com.github.sieves.content.api.tab.TabRegistry
import com.github.sieves.content.upgrade.Upgrade
import com.github.sieves.content.battery.*
import com.github.sieves.content.box.BoxItem
import com.github.sieves.content.box.BoxRenderer
import com.github.sieves.content.box.BoxTile
import com.github.sieves.content.farmer.*
import com.github.sieves.content.forester.*
import com.github.sieves.content.link.LinkItem
import com.github.sieves.content.modules.HungerModule
import com.github.sieves.content.modules.PowerModule
import com.github.sieves.content.modules.SightModule
import com.github.sieves.content.modules.TeleportModule
import com.github.sieves.content.synthesizer.*
import com.github.sieves.registry.internal.*
import com.github.sieves.registry.internal.Registry
import com.github.sieves.registry.internal.net.*
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent
import net.minecraftforge.network.IContainerFactory
import net.minecraftforge.registries.ForgeRegistries

internal object Registry : ListenerRegistry() {

    /**
     * ========================Recipe Serializers registry=========
     */
    object Recipes : Registry<RecipeSerializer<*>>(ForgeRegistries.RECIPE_SERIALIZERS) {
        val SieveSerializer by register("sieve") { Serializer }
    }

    /**
     * ========================RecipeTypes registry=================
     */
    object RecipeTypes :
        MojangRegistry<net.minecraft.core.Registry<RecipeType<*>>, RecipeType<*>>(net.minecraft.core.Registry.RECIPE_TYPE_REGISTRY) {
        val Sieve: RecipeType<SieveRecipe> by register("sieve") {
            object : RecipeType<SieveRecipe> {
                override fun toString(): String {
                    return "sieve".resLoc.toString()
                }
            }
        }

    }

    /**
     * ========================Blocks registry========================
     */
    object Blocks : Registry<Block>(ForgeRegistries.BLOCKS) {
        //        val Sieve by register("sieve") { SieveBlock(BlockBehaviour.Properties.of(Material.STONE)) }
        val Synthesizer by register("synthesizer") { SynthesizerBlock(BlockBehaviour.Properties.of(Material.STONE)) }
        val Battery by register("battery") { BatteryBlock(BlockBehaviour.Properties.of(Material.HEAVY_METAL)) }
        val Box by register("box") { BoxBlock(BlockBehaviour.Properties.of(Material.HEAVY_METAL)) }
        val Farmer by register("farmer") { FarmerBlock(BlockBehaviour.Properties.of(Material.HEAVY_METAL)) }
        val Forester by register("forester") { ForesterBlock(BlockBehaviour.Properties.of(Material.HEAVY_METAL)) }
        val Flux by register("flux_block") { object : Block(Properties.of(Material.HEAVY_METAL)) {} }
    }

    /**
     * ========================Tiles registry========================
     */
    object Tiles : Registry<BlockEntityType<*>>(ForgeRegistries.BLOCK_ENTITIES) {
        //        val Sieve by register("sieve") { tile(Blocks.Sieve) { SieveTile(it.first, it.second) } }
        val Synthesizer by register("synthesizer") { tile(Blocks.Synthesizer) { SynthesizerTile(it.first, it.second) } }
        val Battery by register("battery") { tile(Blocks.Battery) { BatteryTile(it.first, it.second) } }
        val Box by register("box") { tile(Blocks.Box) { BoxTile(it.first, it.second) } }
        val Farmer by register("farmer") { tile(Blocks.Farmer) { FarmerTile(it.first, it.second) } }
        val Forester by register("forester") { tile(Blocks.Forester) { ForesterTile(it.first, it.second) } }
    }

    /**
     * ========================Items registry========================
     */
    object Items : Registry<Item>(ForgeRegistries.ITEMS) {
        val Synthesizer by register("synthesizer") { SynthesizerItem() }
        val Battery by register("battery") { BatteryItem() }
        val Box by register("box") { BoxItem() }
        val Farmer by register("farmer") { FarmerItem() }
        val Forester by register("forester") { ForesterItem() }
        val Linker by register("linker") { LinkItem() }
        val SpeedUpgrade by register("speed") { Upgrade(0, 16) }
        val EfficiencyUpgrade by register("efficiency") { Upgrade(1, 16) }
        val PowerModule by register("power_module") { PowerModule() }
        val HungerModule by register("hunger_module") { HungerModule() }
        val TeleportModule by register("teleport_module") { TeleportModule() }
        val BaseModule by register("base_module") { object : Item(Properties().stacksTo(1).tab(CreativeTab)) {} }
        val SightModule by register("sight_module") { SightModule() }
        val FluxBlockItem by register("flux_block") {
            object : BlockItem(Blocks.Flux, Properties().stacksTo(16).fireResistant().tab(CreativeTab)) {}
        }
        val FluxItem by register("flux_dust") {
            object : Item(Properties().stacksTo(16).tab(CreativeTab)) {}
        }
        val CreativeTab = object : CreativeModeTab("Sieves") {
            override fun makeIcon(): ItemStack = ItemStack(SpeedUpgrade)
        }
    }

    /**
     * ========================Items registry========================
     */
    object Tabs : IRegister {
        val PlayerPower by Tab.register("player_power_tab".resLoc, PowerModule::TagSpec)
        val PlayerHunger by Tab.register("player_hunger_tab".resLoc, HungerModule::TagSpec)
        val PlayerTeleport by Tab.register("player_teleport_tab".resLoc, TeleportModule::TabSpec)
        val PlayerSight by Tab.register("player_sight_tab".resLoc, SightModule::TabSpec)

        override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) =
            TabRegistry.register(modId, modBus, forgeBus)
    }

    /**
     * ========================Keys registry========================
     */
    object Keys : KeyRegistry() {
        val TeleportKey by register("teleport_key", "sieves", InputConstants.KEY_R)
    }

    /**
     * ========================Containers registry========================
     */
    object Containers : Registry<MenuType<*>>(ForgeRegistries.CONTAINERS) {
        //        val Sieve: MenuType<SieveContainer> by register("sieve") {
//            MenuType(IContainerFactory { id, inv, data ->
//                SieveContainer(
//                    id, inv, pos = data.readBlockPos()
//                )
//            })
//        }
        val Synthesizer: MenuType<SynthesizerContainer> by register("synthesizer") {
            MenuType(IContainerFactory { id, inv, data ->
                SynthesizerContainer(
                    id, inv, data.readBlockPos()
                )
            })
        }

        val Battery: MenuType<BatteryContainer> by register("battery") {
            MenuType(IContainerFactory { id, inv, data ->
                BatteryContainer(id, inv, data.readBlockPos())
            })
        }

        val Box: MenuType<BoxContainer> by register("box") {
            MenuType(IContainerFactory { id, inv, data ->
                BoxContainer(id, inv, data.readBlockPos())
            })
        }

        val Farmer: MenuType<FarmerContainer> by register("farmer") {
            MenuType(IContainerFactory { id, inv, data ->
                FarmerContainer(id, inv, data.readBlockPos())
            })
        }
        val Forester: MenuType<ForesterContainer> by register("forester") {
            MenuType(IContainerFactory { id, inv, data ->
                ForesterContainer(id, inv, data.readBlockPos())
            })
        }

    }


    /**
     * Handles our network registrations
     */
    object Net : NetworkRegistry() {
        val Configure by register(0) { ConfigurePacket() }
        val GrowBlock by register(1) { GrowBlockPacket() }
        val HarvestBlock by register(2) { HarvestBlockPacket() }
        val TakeUpgrade by register(3) { TakeUpgradePacket() }
        val SyncTab by register(4) { TabUpdatePacket() }
        val BindTab by register(5) { TabBindPacket() }
        val ClickTab by register(6) { TabClickedPacket() }
        val Teleport by register(7) { TeleportPacket() }
        val SoundEvent by register(8) { PlaySoundPacket() }
        val SightToggle by register(9) { ToggleSightPacket() }
    }

    @Sub
    @OnlyIn(Dist.CLIENT)
    fun onClientSetup(event: FMLClientSetupEvent) {
//        ItemBlockRenderTypes.setRenderLayer(Blocks.Sieve, RenderType.cutoutMipped())
        ItemBlockRenderTypes.setRenderLayer(Blocks.Synthesizer, RenderType.cutoutMipped())
        ItemBlockRenderTypes.setRenderLayer(Blocks.Battery, RenderType.cutoutMipped())
        ItemBlockRenderTypes.setRenderLayer(Blocks.Box, RenderType.cutoutMipped())
        ItemBlockRenderTypes.setRenderLayer(Blocks.Farmer, RenderType.cutoutMipped())
//        MenuScreens.register(Containers.Sieve) { menu, inv, comp -> SieveScreen(menu, inv, comp) }
        MenuScreens.register(Containers.Synthesizer) { menu, inv, _ -> SynthesizerScreen(menu, inv) }
        MenuScreens.register(Containers.Battery) { menu, inv, _ -> BatteryScreen(menu, inv) }
        MenuScreens.register(Containers.Box) { menu, inv, _ -> BoxScreen(menu, inv) }
        MenuScreens.register(Containers.Farmer) { menu, inv, _ -> FarmerScreen(menu, inv) }
        MenuScreens.register(Containers.Forester) { menu, inv, _ -> ForesterScreen(menu, inv) }
    }

    @Sub
    @OnlyIn(Dist.CLIENT)
    fun onRendererRegister(event: RegisterRenderers) {
//        event.registerBlockEntityRenderer(Tiles.Sieve) { SieveRenderer() }
        event.registerBlockEntityRenderer(Tiles.Synthesizer) { SynthesizerRenderer() }
        event.registerBlockEntityRenderer(Tiles.Battery) { BatteryRenderer() }
        event.registerBlockEntityRenderer(Tiles.Box) { BoxRenderer() }
        event.registerBlockEntityRenderer(Tiles.Farmer) { FarmerRenderer() }
        event.registerBlockEntityRenderer(Tiles.Forester) { ForesterRenderer() }
    }

    @Sub
    fun onInterModEnqueue(event: InterModEnqueueEvent) {
        if (ModList.get().isLoaded("theoneprobe")) {
            InterModComms.sendTo("theoneprobe", "getTheOneProbe") { TopPlugin() }
        }
    }


}