package com.github.sieves.api.caps

import net.minecraftforge.energy.EnergyStorage
import net.minecraftforge.fluids.*
import net.minecraftforge.fluids.capability.IFluidHandler.*
import net.minecraftforge.fluids.capability.templates.FluidTank

class TrackedTank(capacity: Int, private val onUpdate: () -> Unit) : FluidTank(capacity) {
    override fun onContentsChanged() = onUpdate()
}