package com.github.sieves.content.machines.forester

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem

/**
 * Our item that will be displayed in game
 */
class ForesterItem : BlockItem(
    Registry.Blocks.Forester,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(1).fireResistant()
)