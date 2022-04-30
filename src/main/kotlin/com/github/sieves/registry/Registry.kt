package com.github.sieves.registry

import com.github.sieves.recipes.SieveRecipe.Serializer
import com.github.sieves.api.tab.Tab
import com.github.sieves.api.tab.TabRegistry
import com.github.sieves.api.tile.*
import com.github.sieves.content.upgrade.Upgrade
import com.github.sieves.content.battery.*
import com.github.sieves.content.farmer.*
import com.github.sieves.content.io.battery.*
import com.github.sieves.content.io.box.*
import com.github.sieves.content.io.fluids.*
import com.github.sieves.content.io.link.LinkItem
import com.github.sieves.content.machines.core.*
import com.github.sieves.content.machines.core.CoreScreen
import com.github.sieves.content.machines.farmer.*
import com.github.sieves.content.machines.forester.*
import com.github.sieves.content.machines.materializer.*
import com.github.sieves.content.machines.synthesizer.*
import com.github.sieves.content.machines.trash.*
import com.github.sieves.content.machines.trash.TrashRenderer
import com.github.sieves.content.modules.*
import com.github.sieves.content.modules.io.*
import com.github.sieves.content.reactor.casing.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.content.reactor.fuel.*
import com.github.sieves.content.reactor.io.*
import com.github.sieves.content.reactor.spark.*
import com.github.sieves.recipes.*
//import com.github.sieves.content.modules.HungerModule
//import com.github.sieves.content.modules.PowerModule
//import com.github.sieves.content.modules.SightModule
//import com.github.sieves.content.modules.TeleportModule
import com.github.sieves.registry.internal.*
import com.github.sieves.registry.internal.Registry
import com.github.sieves.registry.internal.net.*
import com.github.sieves.util.*
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour.*
import net.minecraft.world.level.material.Material
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.network.IContainerFactory
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.runWhenOn

internal object Registry : ListenerRegistry() {

    /**
     * ========================Recipe Serializers registry=========
     */
    object Recipes : Registry<RecipeSerializer<*>>(ForgeRegistries.RECIPE_SERIALIZERS) {
        val SieveSerializer by register("sieve") { Serializer }
        val MaterializerSerializer by register("materializer") { MaterializerRecipe.Serializer }
        val SolidifierSerializer by register("solidifer") { SolidifierRecipe.Serializer }
    }

    /**
     * ========================RecipeTypes registry=================
     */
    object RecipeTypes : MojangRegistry<net.minecraft.core.Registry<RecipeType<*>>, RecipeType<*>>(net.minecraft.core.Registry.RECIPE_TYPE_REGISTRY) {
        val Synthesizer: RecipeType<SieveRecipe> by register("sieve") {
            object : RecipeType<SieveRecipe> {
                override fun toString(): String {
                    return "sieve".resLoc.toString()
                }
            }
        }

        val Materializer: RecipeType<MaterializerRecipe> by register("materializer") {
            object : RecipeType<MaterializerRecipe> {
                override fun toString(): String {
                    return "materializer".resLoc.toString()
                }
            }
        }


        val Solidifier: RecipeType<SolidifierRecipe> by register("solidifer") {
            object : RecipeType<SolidifierRecipe> {
                override fun toString(): String {
                    return "solidifer".resLoc.toString()
                }
            }
        }

    }

    object Sounds : Registry<SoundEvent>(ForgeRegistries.SOUND_EVENTS) {
        val chant by register("chant") {
            SoundEvent("chant".resLoc)
        }
    }

    /**
     * ========================Blocks registry========================
     */
    object Blocks : Registry<Block>(ForgeRegistries.BLOCKS) {
        val Synthesizer by register("synthesizer") { SynthesizerBlock(Properties.of(Material.STONE)) }
        val Battery by register("battery") { BatteryBlock(Properties.of(Material.HEAVY_METAL)) }
        val Box by register("box") { BoxBlock(Properties.of(Material.HEAVY_METAL)) }
        val Fluids by register("tank") { FluidsBlock(Properties.of(Material.HEAVY_METAL)) }
        val Farmer by register("farmer") { FarmerBlock(Properties.of(Material.HEAVY_METAL)) }
        val Forester by register("forester") { ForesterBlock(Properties.of(Material.HEAVY_METAL)) }
        val Materializer by register("materializer") { MaterializerBlock(Properties.of(Material.HEAVY_METAL)) }
        val Trash by register("trash") { TrashBlock(Properties.of(Material.HEAVY_METAL)) }
        val Core by register("core") { CoreBlock(Properties.of(Material.HEAVY_METAL).noOcclusion()) }
        val Flux by register("flux_block") { object : Block(Properties.of(Material.HEAVY_METAL)) {} }
        val Panel by register("panel") { PanelBlock() }
        val Spark by register("spark") { SparkBlock() }
        val Case by register("case") { CasingBlock() }
        val Control by register("control") { ControlBlock() }
        val Input by register("input") { InputBlock() }
        val Output by register("output") { OutputBlock() }
        val Fuel by register("fuel") { FuelBlock() }
    }

    /**
     * ========================Tiles registry========================
     */
    object Tiles : Registry<BlockEntityType<*>>(ForgeRegistries.BLOCK_ENTITIES) {
        val Synthesizer by register("synthesizer") { tile(Blocks.Synthesizer) { SynthesizerTile(it.first, it.second) } }
        val Trash by register("trash") { tile(Blocks.Trash) { TrashTile(it.first, it.second) } }
        val Core by register("core") { tile(Blocks.Core) { CoreTile(it.first, it.second) } }
        val Battery by register("battery") { tile(Blocks.Battery) { BatteryTile(it.first, it.second) } }
        val Box by register("box") { tile(Blocks.Box) { BoxTile(it.first, it.second) } }
        val Fluids by register("tank") { tile(Blocks.Fluids) { FluidsTile(it.first, it.second) } }
        val Farmer by register("farmer") { tile(Blocks.Farmer) { FarmerTile(it.first, it.second) } }
        val Forester by register("forester") { tile(Blocks.Forester) { ForesterTile(it.first, it.second) } }
        val Materializer by register("materializer") { tile(Blocks.Materializer) { MaterializerTile(it.first, it.second) } }
        val Control by register("control") { tile(Blocks.Control) { ControlTile(it.first, it.second) } }
        val Input by register("input") { tile(Blocks.Input) { InputTile(it.first, it.second) } }
        val Output by register("output") { tile(Blocks.Output) { OutputTile(it.first, it.second) } }
        val Spark by register("spark") { tile(Blocks.Spark) { SparkTile(it.first, it.second) } }
//        val Fuel by register("output") { tile(Blocks.Output) { OutputTile(it.first, it.second) } }
    }

    /**
     * ========================Items registry========================
     */
    object Items : Registry<Item>(ForgeRegistries.ITEMS) {
        val Synthesizer by register("synthesizer") { SynthesizerItem() }
        val Trash by register("trash") { TrashItem() }
        val Core by register("core") { CoreItem() }
        val Battery by register("battery") { BatteryItem() }
        val Box by register("box") { BoxItem() }
        val Fluids by register("tank") { FluidsItem() }
        val Farmer by register("farmer") { FarmerItem() }
        val Forester by register("forester") { ForesterItem() }
        val Panel by register("panel") { object : BlockItem(Blocks.Panel, Properties().fireResistant().tab(CreativeTab).stacksTo(64)) {} }
        val Spark by register("spark") { object : BlockItem(Blocks.Spark, Properties().fireResistant().tab(CreativeTab).stacksTo(64)) {} }
        val Case by register("case") { object : BlockItem(Blocks.Case, Properties().fireResistant().tab(CreativeTab).stacksTo(64)) {} }
        val Control by register("control") { object : BlockItem(Blocks.Control, Properties().fireResistant().tab(CreativeTab).stacksTo(64)) {} }
        val Input by register("input") { object : BlockItem(Blocks.Input, Properties().fireResistant().tab(CreativeTab).stacksTo(64)) {} }
        val Output by register("output") { object : BlockItem(Blocks.Output, Properties().fireResistant().tab(CreativeTab).stacksTo(64)) {} }
        val Fuel by register("fuel") { object : BlockItem(Blocks.Fuel, Properties().fireResistant().tab(CreativeTab).stacksTo(64)) {} }
        val Materializer by register("materializer") { MaterializerItem() }
        val SpeedUpgrade by register("speed") { Upgrade(0, 16) }
        val EfficiencyUpgrade by register("efficiency") { Upgrade(1, 16) }
        val BaseModule by register("base_module") { object : Item(Properties().stacksTo(1).tab(CreativeTab)) {} }
        val StepModule by register("step_module") { StepModule() }
        val FlightModule by register("flight_module") { FlightModule() }
        val SightModule by register("sight_module") { SightModule() }
        val PowerModule by register("power_module") { PowerModule() }
        val HungerModule by register("hunger_module") { HungerModule() }
        val TeleportModule by register("teleport_module") { TeleportModule() }

        //        val ScareModule by register("scare_module") { ScareModule() }
        val ExportModule by register("export_module") { ExportModule() }
        val Flux by register("flux_block") { object : BlockItem(Blocks.Flux, Properties().fireResistant().stacksTo(64).tab(CreativeTab)) {} }
        val FluxDust by register("flux_dust") { object : Item(Properties().tab(CreativeTab).fireResistant().stacksTo(64)) {} }
        val Linker by register("linker") { LinkItem() }
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
        val PlayerStep by Tab.register("player_step_tab".resLoc, StepModule::TabSpec)
        val PlayerFlight by Tab.register("player_flight_tab".resLoc, FlightModule::TabSpec)

        //        val PlayerScare by Tab.register("player_scare_tab".resLoc, ScareModule::TabSpec)
        val PlayerExport by Tab.register("player_export_tab".resLoc, ExportModule::TabSpec)

        override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) = TabRegistry.register(modId, modBus, forgeBus)
    }

    /**
     * ========================Keys registry========================
     */
    object Keys : KeyRegistry() {
        val TeleportKey by register("teleport_key", "synth", InputConstants.KEY_R)
    }

    /**
     * ========================Containers registry========================
     */
    object Containers : Registry<MenuType<*>>(ForgeRegistries.CONTAINERS) {
        val Synthesizer: MenuType<SynthesizerContainer> by register("synthesizer") {
            MenuType(IContainerFactory { id, inv, data ->
                SynthesizerContainer(id, inv, data.readBlockPos())
            })
        }

        val Trash: MenuType<TrashContainer> by register("trash") {
            MenuType(IContainerFactory { id, inv, data ->
                TrashContainer(id, inv, data.readBlockPos())
            })
        }

        val Core: MenuType<CoreContainer> by register("core") {
            MenuType(IContainerFactory { id, inv, data ->
                CoreContainer(id, inv, data.readBlockPos())
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
        val Fluids: MenuType<FluidsContainer> by register("tank") {
            MenuType(IContainerFactory { id, inv, data ->
                FluidsContainer(id, inv, data.readBlockPos())
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

        val Materializer: MenuType<MaterializerContainer> by register("materializer") {
            MenuType(IContainerFactory { id, inv, data ->
                MaterializerContainer(id, inv, data.readBlockPos())
            })
        }

        val Filter: MenuType<FilterContainer> by register("filter") {
            MenuType(IContainerFactory { id, inv, data ->
                FilterContainer(id, inv, data.readItem())
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
        val DeleteItem by register(10) { DeleteItemPacket() }
        val StepToggle by register(11) { ToggleStepPacket() }
        val FlightToggle by register(12) { FlightPacket() }
        val MenuOpen by register(13) { MenuStatePacket() }
        val MaterializerStart by register(14) { StartMaterializer() }
        val MaterializerStop by register(15) { StopMaterializer() }
        val SolidiferStart by register(16) { StartSolidifer() }
        val SolidiferStop by register(17) { StopSolidifer() }
    }


    @Sub
    @OnlyIn(Dist.CLIENT)
    fun onClientSetup(event: FMLClientSetupEvent) {
        runWhenOn(Dist.CLIENT) {
            ItemBlockRenderTypes.setRenderLayer(Blocks.Synthesizer, RenderType.cutoutMipped())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Trash, RenderType.cutoutMipped())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Battery, RenderType.cutoutMipped())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Box, RenderType.cutoutMipped())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Fluids, RenderType.translucent())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Core, RenderType.cutout())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Panel, RenderType.cutout())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Spark, RenderType.cutout())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Case, RenderType.translucent())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Input, RenderType.translucent())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Output, RenderType.translucent())
            ItemBlockRenderTypes.setRenderLayer(Blocks.Fuel, RenderType.translucent())
            MenuScreens.register(Containers.Synthesizer) { menu, inv, _ -> SynthesizerScreen(menu, inv) }
            MenuScreens.register(Containers.Trash) { menu, inv, _ -> TrashScreen(menu, inv) }
            MenuScreens.register(Containers.Core) { menu, inv, _ -> CoreScreen(menu, inv) }
            MenuScreens.register(Containers.Battery) { menu, inv, _ -> BatteryScreen(menu, inv) }
            MenuScreens.register(Containers.Box) { menu, inv, _ -> BoxScreen(menu, inv) }
            MenuScreens.register(Containers.Fluids) { menu, inv, _ -> FluidsScreen(menu, inv) }
            MenuScreens.register(Containers.Farmer) { menu, inv, _ -> FarmerScreen(menu, inv) }
            MenuScreens.register(Containers.Forester) { menu, inv, _ -> ForesterScreen(menu, inv) }
            MenuScreens.register(Containers.Materializer) { menu, inv, _ -> MaterializerScreen(menu, inv) }
            MenuScreens.register(Containers.Filter) { menu, inv, _ -> FilterScreen(menu, inv) }
        }

    }

    @Sub
    @OnlyIn(Dist.CLIENT)
    fun clientOnRendererRegister(event: RegisterRenderers) {
        runWhenOn(Dist.CLIENT) {
            Net.DeleteItem.clientListener(TrashRenderer::netListener)
            Net.HarvestBlock.clientListener(FarmerRenderer::onHarvestBlock)
            Net.GrowBlock.clientListener(FarmerRenderer::onGrowBlock)
            Net.HarvestBlock.clientListener(ForesterRenderer::onHarvestBlock)
            Net.GrowBlock.clientListener(ForesterRenderer::onGrowBlock)

            Net.SolidiferStart.clientListener(CoreRenderer::onStart)
            Net.SolidiferStop.clientListener(CoreRenderer::onStop)
            event.registerBlockEntityRenderer(Tiles.Synthesizer) { SynthesizerRenderer() }
            event.registerBlockEntityRenderer(Tiles.Trash) { TrashRenderer() }
            event.registerBlockEntityRenderer(Tiles.Core) { CoreRenderer(it) }
            event.registerBlockEntityRenderer(Tiles.Battery) { BatteryRenderer() }
            event.registerBlockEntityRenderer(Tiles.Box) { BoxRenderer() }
            event.registerBlockEntityRenderer(Tiles.Fluids) { FluidsRenderer() }
            event.registerBlockEntityRenderer(Tiles.Farmer) { FarmerRenderer() }
            event.registerBlockEntityRenderer(Tiles.Forester) { ForesterRenderer() }
            event.registerBlockEntityRenderer(Tiles.Materializer) { MaterializerRenderer() }
        }
    }

    /**
     * Handles the custom interaction on right click even when shiftclicking
     */
    @Sub
    fun onPlayerInteract(event: PlayerInteractEvent) {
//        val player = event.player
//        val level = player.level
//        val be = level.getBlockEntity(event.pos) ?: return
//        if (be is IMultiBlock<*>) {
//            if (event.side.isClient) event.face?.let { event.cancellationResult = be.onUseClient(player, event.itemStack, it) }
//            if (event.side.isServer) event.face?.let { event.cancellationResult = be.onUseServer(player as ServerPlayer, event.itemStack, it) }
//        }
    }


}