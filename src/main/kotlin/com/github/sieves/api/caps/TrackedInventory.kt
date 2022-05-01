package com.github.sieves.api.caps

import com.github.sieves.api.tile.*
import net.minecraftforge.items.ItemStackHandler

class TrackedInventory(size: Int, override val update: () -> Unit) : ItemStackHandler(size), IDelegateHandler {

    override fun onContentsChanged(slot: Int) = update()
}