package com.github.sieves.content.box

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem

/**
 * Our item that will be displayed in game
 */
class BoxItem : BlockItem(
    Registry.Blocks.Box,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(1).fireResistant()
)