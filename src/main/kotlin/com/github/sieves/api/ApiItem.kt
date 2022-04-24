package com.github.sieves.api

import com.github.sieves.registry.Registry
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import kotlin.math.max

abstract class ApiItem(maxStack: Int) : Item(Properties().stacksTo(maxStack).fireResistant().tab(Registry.Items.CreativeTab)) {

}