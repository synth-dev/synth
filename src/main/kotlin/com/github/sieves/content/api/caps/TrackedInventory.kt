package com.github.sieves.content.api.caps

import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.ItemStackHandler

class TrackedInventory(size: Int, private val onUpdate: () -> Unit) : ItemStackHandler(size) {
    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        val result = super.insertItem(slot, stack, simulate)
        onUpdate()
        return result
    }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        val result = super.extractItem(slot, amount, simulate)
        onUpdate()
        return result
    }


}