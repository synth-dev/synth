package com.github.sieves.content.machines.farmer

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab

/**
 * Our item that will be displayed in game
 */
class FarmerItem : BlockItem(
    Registry.Blocks.Farmer,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(1).fireResistant()
)