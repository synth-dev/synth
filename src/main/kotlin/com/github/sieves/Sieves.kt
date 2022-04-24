package com.github.sieves

import com.github.sieves.registry.Registry
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(Sieves.ModId)
object Sieves {
    const val ModId: String = "synth"

    init {
        Registry.register(ModId, MOD_BUS, FORGE_BUS)
    }
}