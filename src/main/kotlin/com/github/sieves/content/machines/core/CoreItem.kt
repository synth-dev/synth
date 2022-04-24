package com.github.sieves.content.machines.core

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem

/**
 * Our item that will be displayed in game
 */
class CoreItem : BlockItem(
    Registry.Blocks.Core,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(1).fireResistant()
)