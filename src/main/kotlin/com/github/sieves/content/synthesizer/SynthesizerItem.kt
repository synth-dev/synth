package com.github.sieves.content.synthesizer

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab

/**
 * Our item that will be displayed in game
 */
class SynthesizerItem : BlockItem(
    Registry.Blocks.Synthesizer,
    Properties().tab(Registry.Items.CreativeTab).stacksTo(4).fireResistant()
)