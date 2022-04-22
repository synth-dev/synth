package com.github.sieves.registry.internal

import com.github.sieves.util.Log.info
import com.google.common.collect.Queues
import net.minecraft.resources.ResourceKey
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.ForgeConfig.Client
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.IModBusEvent
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.network.IContainerFactory
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.RegistryObject
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*


abstract class Registry<B : IForgeRegistryEntry<B>>(private val registry: IForgeRegistry<B>) : IRegister {
    private lateinit var deferredRegister: DeferredRegister<B>
    private val registers: Queue<Pair<String, () -> B>> = Queues.newArrayDeque()
    private val objects: MutableMap<String, RegistryObject<B>> = HashMap()

    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        deferredRegister = DeferredRegister.create(registry, modId)
        while (registers.peek() != null) {
            val data = registers.remove()
            objects[data.first] = deferredRegister.register(data.first, data.second)
        }
        deferredRegister.register(modBus)
        registerListeners(modBus, forgeBus)
    }


    /**
     * This is used delegate registration using properties
     */
    protected fun <T : B> register(
        name: String, supplier: () -> T
    ): ReadOnlyProperty<Any?, T> {
        registers.add(name to supplier)
        return object : ReadOnlyProperty<Any?, T>, Supplier<T>, () -> T {
            override fun get(): T = objects[name]!!.get() as T

            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

            override fun invoke(): T = get()
        }
    }

    private fun registerListeners(modBus: IEventBus, forgeBus: IEventBus) {
        for (member in this::class.functions) {
            if (member.visibility != KVisibility.PUBLIC) continue
            if (member.hasAnnotation<Sub>()) {
                val side = member.findAnnotation<Sub>()?.dist
                if (side == Side.Client && FMLEnvironment.dist != Dist.CLIENT) continue
                if (side == Side.Server && FMLEnvironment.dist != Dist.DEDICATED_SERVER) continue
                for (param in member.parameters) {
                    val type = param.type.classifier as KClass<*>
                    if (type.isSubclassOf(Event::class)) {
                        val modType: Class<out Event> = type.java as Class<out Event>
                        if (type.isSubclassOf(IModBusEvent::class)) modBus.addListener(
                            EventPriority.LOWEST,
                            true,
                            modType
                        ) {
                            member.call(this, it)
                        }
                        else forgeBus.addListener(EventPriority.LOWEST, true, modType) {
                            member.call(this, it)
                        }
                        info { "Found mod bus event named '${member.name}', with event type '${modType.simpleName}'" }
                        continue
                    }
                }
            }
        }
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Sub(val dist: Side = Side.Both)

enum class Side() {
    Client, Server, Both
}

//ResourceKey<Registry<RecipeType<?>>>
abstract class MojangRegistry<R : net.minecraft.core.Registry<T>, T>(private val registry: ResourceKey<R>) :
    IRegister {
    private lateinit var deferredRegister: DeferredRegister<T>
    private val registers: Queue<Pair<String, () -> T>> = Queues.newArrayDeque()
    private val objects: MutableMap<String, RegistryObject<T>> = HashMap()

    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        deferredRegister = DeferredRegister.create(registry, modId)
        while (registers.peek() != null) {
            val data = registers.remove()
            objects[data.first] = deferredRegister.register(data.first, data.second)
        }
        deferredRegister.register(modBus)
    }


    /**
     * This is used delegate registration using properties
     */
    protected fun <B : T> register(
        name: String, supplier: () -> B
    ): ReadOnlyProperty<Any?, B> {
        registers.add(name to supplier)
        return object : ReadOnlyProperty<Any?, B>, Supplier<B>, () -> B {
            override fun get(): B = objects[name]!!.get() as B

            override fun getValue(thisRef: Any?, property: KProperty<*>): B = get()

            override fun invoke(): B = get()
        }
    }
}