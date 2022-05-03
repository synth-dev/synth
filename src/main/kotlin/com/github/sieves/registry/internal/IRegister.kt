package com.github.sieves.registry.internal

import com.github.sieves.dsl.Log
import com.github.sieves.dsl.registerAll
import net.minecraftforge.eventbus.api.IEventBus
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

interface IRegister {
    fun register(modId: String, modBus: IEventBus = MOD_BUS, forgeBus: IEventBus = FORGE_BUS) {
        registerAll()
        Log.debug { "Bypassing register (for registry variables to be statically loaded)s" }
    }
}