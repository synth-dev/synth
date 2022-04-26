package com.github.sieves.content.machines.materializer

import com.github.sieves.registry.Registry.Blocks
import com.github.sieves.registry.Registry.Items.CreativeTab
import net.minecraft.world.item.*

class MaterializerItem : BlockItem(
    Blocks.Materializer,
    Properties().tab(CreativeTab).stacksTo(1).fireResistant()
)