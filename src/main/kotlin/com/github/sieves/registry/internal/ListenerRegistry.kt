package com.github.sieves.registry.internal

import com.github.sieves.dsl.*
import com.github.sieves.dsl.Log.info
import net.minecraftforge.api.distmarker.*
import net.minecraftforge.eventbus.api.*
import net.minecraftforge.fml.event.*
import net.minecraftforge.fml.loading.*
import kotlin.reflect.*
import kotlin.reflect.full.*

abstract class ListenerRegistry : IRegister {
    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        for (member in this::class.declaredFunctions) {
            info { "loading: ${member.name}" }
            if (member.visibility != KVisibility.PUBLIC) continue
            val dist = if (member.name.startsWith("client")) Dist.CLIENT else if (member.name.startsWith("server")) Dist.DEDICATED_SERVER else null
            if (dist != null && dist != FMLEnvironment.dist) continue
            for (param in member.parameters) {
                val type = param.type.classifier as KClass<*>
                if (type.isSubclassOf(Event::class)) {
                    val modType: Class<out Event> = type.java as Class<out Event>
                    if (type.isSubclassOf(IModBusEvent::class)) {
                        modBus.addListener(EventPriority.LOWEST, true, modType) {
                            member.call(this, it)
                        }
                    } else forgeBus.addListener(EventPriority.LOWEST, true, modType) {
                        println("REGISTERING $it")
                        member.call(this, it)
                    }
                    continue
                }
            }
        }
        registerAll()
    }

}