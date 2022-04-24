package com.github.sieves.api.caps

import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.ItemStackHandler

class TrackedInventory(size: Int, private val onUpdate: () -> Unit) : ItemStackHandler(size) {
    override fun onContentsChanged(slot: Int) = onUpdate()
}