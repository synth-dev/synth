package com.github.sieves.content.reactor.core

import com.github.sieves.api.multiblock.*
import com.github.sieves.api.tile.*
import com.github.sieves.content.io.link.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.content.reactor.core.ChamberTile.ChamberState.*
import com.github.sieves.dsl.*
import com.github.sieves.registry.Registry.Tiles
import mcjty.theoneprobe.api.*
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.state.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*
import net.minecraftforge.items.*
import software.bernie.geckolib3.core.*
import software.bernie.geckolib3.core.PlayState.*
import software.bernie.geckolib3.core.builder.*
import software.bernie.geckolib3.core.event.predicate.*
import software.bernie.geckolib3.network.*

class ChamberTile(pos: BlockPos, state: BlockState) : AnimatedTile<ChamberTile>(Tiles.Chamber, pos, state), ISlave<ControlTile, ChamberTile>, ISyncable {

    /**Get and set the master instance**/
    override var master: Opt<IMaster<ControlTile>> = Opt.nil()

    /**Used for interactions with the master**/
    override var store: Opt<StructureStore> = Opt.nil()

    /**Creates a single slot buffer, the items will be used up over a period of time,
     * when that time is over it will be extracted/deleted allowing for the input tile to insert another
     * piece of fuel**/
    internal val items by handlerOf<Delegates.Items>("items", 1)

    /**Creates a MASSIVE size energy buffer (2,147,483,647 FE)**/
    private val energy by handlerOf<Delegates.Energy>("energy", Int.MAX_VALUE)

    /**Keep track of the linked spark tiles**/
    private val links = Links()

    /**The speed completely depends on the number of links we have**/
    private val linkCount: Int get() = links.size

    /**The power output of the reactor, defined by 75000 fe * the number of spark blocks (links) **/
    private val output: Int get() = linkCount * 75000

    /**The amount of time (in ticks) that the fuel will burn for before being consumed**/
    private val burnTime: Int get() = (20 * 60)

    /**Keep track of our current state**/
    private var chamberState: ChamberState = Idle

    /**Track the current state's time on the server**/
    private var currentTime: Int = 0

    /**
     * Generate our power/update state
     */
    override fun onTick(level: Level) {
        if (level.isClientSide) return
        //todo: move this to respective enums as constant values after we figure them out
        if (chamberState == Starting && currentTime >= 50) setState(Active)
        if (chamberState == Stopping && currentTime >= 50) setState(Idle)
        currentTime++

        val item = items().getStackInSlot(0)
        val stack = items map { getStackInSlot(0) }
    }

    /**
     * Updates the state and pushes it to the client via the [update] method
     */
    private fun setState(new: ChamberState) {
        this.chamberState = new
        this.currentTime = 0
        update()
    }

    /**
     * This will start the power up animation.
     * Only will start if it's currently idle,
     */
    fun powerUp(): Boolean {
        if (!master) return false
        setState(Starting)
        return true
    }

    /**
     * This will start the power up animation.
     * Only will start if it's currently idle,
     */
    fun powerDown(): Boolean {
        setState(Stopping)
        return true
    }

    /**
     * Link to a given block position
     */
    fun addLink(blockPos: BlockPos) {
        links.add(blockPos)
        update()
    }

    /**
     * Remove all the links to this reactor core
     */
    fun purgeLinks() {
        links.removeLinks()
        update()
    }

    /**
     * Delegate the animation through this piped event
     */
    override fun AnimationEvent<ChamberTile>.animate(): PlayState {
        when (animatable.chamberState) {
            Idle -> {
                controller.transitionLengthTicks = 0.0
                controller.setAnimation(AnimationBuilder().addAnimation("animation.chamber.idle"))
            }
            Starting -> {
                controller.transitionLengthTicks = 0.0
                controller.setAnimation(AnimationBuilder().addAnimation("animation.chamber.starting").addAnimation("animation.chamber.ready"))
            }
            Active -> {
                controller.transitionLengthTicks = 10.0
                controller.setAnimation(AnimationBuilder().addAnimation("animation.chamber.active"))
            }
            Stopping -> {
                controller.transitionLengthTicks = 10.0
                controller.setAnimation(AnimationBuilder().addAnimation("animation.chamber.stopping"))
            }
        }
        return CONTINUE
    }

    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return items.cast()
        return super.getCapability(cap, side)
    }

    /**
     * Renders our current state
     */
    override fun renderTop(info: IProbeInfo, mode: ProbeMode, data: IProbeHitData, player: Player) {
        super.renderTop(info, mode, data, player)
        info.text("State: ${chamberState.name.spaced()}")
    }

    override fun onSave(tag: CompoundTag) {
        tag.putEnum("chamber_state", chamberState)
    }

    override fun onLoad(tag: CompoundTag) {
        chamberState = tag.getEnum("chamber_state")
    }

    override fun onUse(level: Level, player: Player, itemUsed: ItemStack, direction: Direction): InteractionResult {
//        if (level.isClientSide) InteractionResult.sidedSuccess(level.isClientSide)
//        chamberState++
//        return InteractionResult.sidedSuccess(level.isClientSide)
        return super.onUse(level, player, itemUsed, direction)
    }

    /**
     * Keep track of the current state of the reactor
     */
    enum class ChamberState() {
        Idle, Starting, Active, Stopping,
    }

    /**
     * Called upon syncing of the client
     */
    override fun onAnimationSync(id: Int, state: Int) {

    }
}