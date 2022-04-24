package com.github.sieves.content.machines.trash

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem

/**
 * Our item that will be displayed in game
 */
class TrashItem : BlockItem(
    Registry.Blocks.Trash,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(1).fireResistant()
)