package com.github.sieves.content.io.fluids

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem

/**
 * Our item that will be displayed in game
 */
class FluidsItem : BlockItem(
    Registry.Blocks.Fluids,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(1).fireResistant()
)