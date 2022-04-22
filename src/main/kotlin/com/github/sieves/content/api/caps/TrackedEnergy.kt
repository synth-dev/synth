package com.github.sieves.content.api.caps

import net.minecraftforge.energy.EnergyStorage

class TrackedEnergy(capacity: Int, private val onUpdate: () -> Unit) : EnergyStorage(capacity) {
    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        val result = super.receiveEnergy(maxReceive, simulate)
        onUpdate()
        return result
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        val result = super.extractEnergy(maxExtract, simulate)
        onUpdate()
        return result
    }
}