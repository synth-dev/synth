package com.github.sieves.registry.internal

import com.github.sieves.Sieves
import com.github.sieves.dsl.Log
import net.minecraft.client.KeyMapping
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.ClientRegistry
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.runWhenOn
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class KeyRegistry : IRegister {
    private val mappings = HashMap<String, KeyMapping>()

    /**
     * This is used delegate registration using properties
     */
    protected fun register(
        name: String, category: String, keycode: Int
    ): ReadOnlyProperty<Any?, KeyMapping> {
        runWhenOn(Dist.CLIENT) {
            val mapping = KeyMapping("${Sieves.ModId}.$name", keycode, category)
            mappings[name] = (mapping)
        }
        return object : ReadOnlyProperty<Any?, KeyMapping>, Supplier<KeyMapping>, () -> KeyMapping {
            override fun get(): KeyMapping = mappings[name]!!
            override fun getValue(thisRef: Any?, property: KProperty<*>): KeyMapping = get()

            override fun invoke(): KeyMapping = get()
        }
    }

    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        modBus.addListener<FMLClientSetupEvent> {
            mappings.forEach {
                ClientRegistry.registerKeyBinding(it.value)
                Log.info { "Registered key mapping: ${it.value.name}" }
            }
        }
    }
}