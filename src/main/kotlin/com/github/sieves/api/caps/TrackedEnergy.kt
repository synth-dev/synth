package com.github.sieves.api.caps

import com.github.sieves.api.tile.*
import net.minecraftforge.energy.EnergyStorage

class TrackedEnergy(capacity: Int, override val update: () -> Unit) : EnergyStorage(capacity), IDelegateHandler {

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        val result = super.receiveEnergy(maxReceive, simulate)
        update()
        return result
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        val result = super.extractEnergy(maxExtract, simulate)
        update()
        return result
    }

}