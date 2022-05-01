package com.github.sieves.api.tile

import net.minecraftforge.energy.*
import net.minecraftforge.fluids.capability.templates.*
import net.minecraftforge.items.*
import java.util.function.*

object Delegates {
    class Energy(capacity: Int, private val tile: Supplier<ITile<*>>) : EnergyStorage(capacity) {
        override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
            val result = super.receiveEnergy(maxReceive, simulate)
            tile.get().update()
            return result
        }

        override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
            val result = super.extractEnergy(maxExtract, simulate)
            tile.get().update()
            return result
        }
    }

    class Items(size: Int, private val tile: Supplier<ITile<*>>) : ItemStackHandler(size) {
        override fun onContentsChanged(slot: Int) = tile.get().update()
    }

    class Fluids(
        capacity: Int, private val tile: Supplier<ITile<*>>
    ) : FluidTank(capacity) {

        override fun onContentsChanged() = tile.get().update()

    }
}