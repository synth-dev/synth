package com.github.sieves.api.caps

import com.github.sieves.api.tile.*
import net.minecraftforge.fluids.capability.templates.FluidTank

class TrackedTank(
    capacity: Int, override val update: () -> Unit
) : FluidTank(capacity), IDelegateHandler {
    override fun onContentsChanged() = update()

}