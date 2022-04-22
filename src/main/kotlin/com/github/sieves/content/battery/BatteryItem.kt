package com.github.sieves.content.battery

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab

/**
 * Our item that will be displayed in game
 */
class BatteryItem : BlockItem(
    Registry.Blocks.Battery,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(1).fireResistant()
)