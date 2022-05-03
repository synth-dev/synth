package com.github.sieves.api.tile

import com.github.sieves.dsl.*
import com.github.sieves.dsl.Log.warn
import net.minecraft.client.*
import net.minecraft.core.*
import net.minecraft.resources.*
import net.minecraft.sounds.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import software.bernie.geckolib3.core.*
import software.bernie.geckolib3.core.PlayState.*
import software.bernie.geckolib3.core.controller.*
import software.bernie.geckolib3.core.event.*
import software.bernie.geckolib3.core.event.predicate.*
import software.bernie.geckolib3.core.manager.*

abstract class AnimatedTile<T>(type: BlockEntityType<T>, pos: BlockPos, state: BlockState) : BaseTile<T>(type, pos, state),
    IAnimatable where T : BlockEntity, T : IAnimatable {
    @Suppress("LeakingThis")
    private val factory: AnimationFactory = AnimationFactory(this)

    /**The default name for controller**/
    protected open val animatorName: String = "controller"

    /**The transition length between animations**/
    protected open val animatorTickTransition: Float = 0f

    /**
     * registers our listeners and controller
     */
    override fun registerControllers(data: AnimationData) {
        val controller = AnimationController(this, animatorName, animatorTickTransition, this::predicate)
        controller.registerParticleListener { it.onParticleEvent() }
        controller.registerSoundListener { it.onSoundEvent() }
        controller.registerCustomInstructionListener { it.onCustomEvent() }
        data.addAnimationController(controller)
    }

    /**
     * This will take the event and cast it to the predicate
     */
    @Suppress("UNCHECKED_CAST")
    private fun <E> predicate(event: AnimationEvent<E>): PlayState where E : BlockEntity, E : IAnimatable {
        if (this::class.isInstance(event.animatable)) return (event as AnimationEvent<T>).animate()
        warn { "Failed to cast the animation event!" }
        return STOP
    }

    /**
     * Delegate the animation through this piped event
     */
    protected abstract fun AnimationEvent<T>.animate(): PlayState

    /**
     * Adds an event to animate particles
     */
    protected open fun ParticleKeyFrameEvent<AnimatedTile<T>>.onParticleEvent() {
    }

    /**
     * Adds events for sounds within the tile
     */
    protected open fun SoundKeyframeEvent<AnimatedTile<T>>.onSoundEvent() {
        val parsed = parseSound(this.sound)
        if (!parsed || !world) return //Important or will cause NoElementFoundExeception
        if (!world().isClientSide) return
        val player = Minecraft.getInstance().player!!
        val playerPos = player.position()
        val blockPos = this@AnimatedTile.blockPos
        val dist = (playerPos.distanceTo(Vec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)))
        val volume = (parsed map { it.second.first }) / (dist.toFloat() * 0.5f)
        if (volume >= 0.08f)
            player.playSound(
                parsed map { it.first },
                volume,
                parsed map { it.second.second },
            )
    }

    /**
     * Parses out a sound from the sound tag within an animation
     */
    private fun parseSound(sound: String): Opt<Pair<SoundEvent, Pair<Float, Float>>> {
        var target = sound
        var pitch = 1f
        var volume = 1f
        if (target.contains(";")) {
            val split = target.split(";")
            target = split[0]
            if (split.size == 2) {
                volume = split[1].trim().replace("([a-z])".toRegex(), "").toFloatOrNull() ?: 1f
            } else if (split.size == 3) {
                volume = split[1].trim().replace("([a-z])".toRegex(), "").toFloatOrNull() ?: 1f
                pitch = split[2].trim().replace("([a-z])".toRegex(), "").toFloatOrNull() ?: 1f
            }
        }
        val soundEvent = Registry.SOUND_EVENT.get(ResourceLocation(target)) ?: return Opt.nil()
        return opt(soundEvent to (volume to pitch))
    }


    /**
     * Adds events for custom instructions within the tile
     */
    protected open fun CustomInstructionKeyframeEvent<AnimatedTile<T>>.onCustomEvent() {}

    /**
     * Return our factory instance
     */
    override fun getFactory(): AnimationFactory = factory
}