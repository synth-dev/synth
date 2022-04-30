package com.github.sieves.content.reactor.spark

import com.github.sieves.api.tile.*
import com.github.sieves.content.io.link.*
import com.github.sieves.content.reactor.control.*
import com.github.sieves.registry.*
import com.github.sieves.registry.Registry.Items
import com.github.sieves.util.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.*
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

/**
 * Keeps track of the internal input buffer of items being pumped into the controller
 */
class SparkTile(blockPos: BlockPos, blockState: BlockState) : ReactorTile<SparkTile>(Registry.Tiles.Spark, blockPos, blockState) {
    private var tick = 0
    private val outgoingLinks = Links()
    private val incomingLinks = Links()
    val isRoot: Boolean
        get() = if (ctrl.isAbsent) false else {
            ctrl().sparks.contains(blockPos)
        }

    /**
     * Will only tick when the controller is present
     */
    override fun onServerTick() = ctrl.ifPresent {
        if (tick >= 20) {
            validateLinks(incomingLinks, it)
            validateLinks(outgoingLinks, it)
        }
        tick++
    }

    /**
     * Validates/purges any invalid spark tiles.
     */
    private fun validateLinks(links: Links, controlTile: ControlTile) {
        for (link in links) {
            val tile = level?.getBlockEntity(link)
            if (tile !is SparkTile) {
                //Send the message that a given block was broken to all players inside the multiblock
                val players = level?.getEntitiesOfClass(Player::class.java, controlTile.multiBlock.aabb)
                players?.forEach { player ->
                    (player as ServerPlayer).sendMessage(
                        TextComponent("Removed all link to ${link.toShortString()}, from ${blockPos.toShortString()}"),
                        ChatType.GAME_INFO,
                        UUID.randomUUID()
                    )
                }
                links.queueRemove(link)
            }
        }
        links.poll()
    }

    override fun onUseServer(player: ServerPlayer, itemUsed: ItemStack, direction: Direction): InteractionResult {
        if (itemUsed.`is`(Items.Linker)) if (attemptLink(
                itemUsed,
                player
            )
        ) return InteractionResult.sidedSuccess(true)
        return super.onUseServer(player, itemUsed, direction)
    }

    /**
     * attempts to link to another node, or start a new connection if not preset
     */
    private fun attemptLink(linker: ItemStack, player: ServerPlayer): Boolean {
        if (player.isShiftKeyDown) {
            outgoingLinks.forEach<SparkTile>(player.level) {
                it.incomingLinks.removeLink(this.blockPos)
            }
            incomingLinks.forEach<SparkTile>(player.level) {
                it.outgoingLinks.removeLink(this.blockPos)
            }
            incomingLinks.removeLinks()
            outgoingLinks.removeLinks()
            linker.tag?.remove("from")
            linker.tag?.putBoolean("foil", false)
            player.sendMessage(TextComponent("Removed all links from spark at ${blockPos.toShortString()}"), ChatType.GAME_INFO, UUID.randomUUID())
            return true
        } else {
            val tag = linker.tag ?: return attemptNewLink(linker, player)
            if (!tag.contains("from")) return attemptNewLink(linker, player)
            return establishLink(linker, player)
        }
    }

    //At this point it's assumed that we have a valid from block position
    private fun establishLink(linker: ItemStack, player: ServerPlayer): Boolean {
        val from = linker.tag?.getBlockPos("from") ?: return false
        if (!sameAxis(from.bp)) {
            player.sendMessage(
                TextComponent("Spark links must be within a straight line, either vertical or horizontal"),
                ChatType.GAME_INFO,
                UUID.randomUUID()
            )
            return false
        }
        if (outgoingLinks.contains(from)) {
            player.sendMessage(TextComponent("These two sparks are already linked!"), ChatType.GAME_INFO, UUID.randomUUID())
            return false
        }
        val fromSpark = level?.getBlockEntity(from.bp) ?: return false
        if (fromSpark !is SparkTile) return false
        if (fromSpark.outgoingLinks.contains(blockPos)) {
            player.sendMessage(TextComponent("These two sparks are already linked!"), ChatType.GAME_INFO, UUID.randomUUID())
            return false
        }
        fromSpark.outgoingLinks.addLink(this.blockPos)
        incomingLinks.addLink(from.bp)
        linker.tag = CompoundTag() //Reset the tag
        player.sendMessage(TextComponent("Established link from ${from.toShortString()} to ${blockPos.toShortString()}"), ChatType.GAME_INFO, UUID.randomUUID())
        return true
    }

    /**
     * Checks to see if the given blockPos shares at least one axis with this spark
     */
    private fun sameAxis(blockPos: BlockPos): Boolean {
        return (blockPos.x == this.blockPos.x && blockPos.y == this.blockPos.y) || (blockPos.x == this.blockPos.x && blockPos.z == this.blockPos.z) || (blockPos.y == this.blockPos.y && blockPos.z == this.blockPos.z)
    }


    /**
     * Will generate the new item tag with the "from" attribute as this spark
     */
    private fun attemptNewLink(linker: ItemStack, player: ServerPlayer): Boolean {
        if (ctrl.isAbsent) {
            player.sendMessage(
                TextComponent("No viable path to input block!"),
                ChatType.GAME_INFO,
                UUID.randomUUID()
            )
            return false
        }
        if (linkedFromRoot()) {
            val fromTag = linker.orCreateTag
            fromTag.putBlockPos("from", this.blockPos)
            player.sendMessage(TextComponent("Started new spark link from ${blockPos.toShortString()}"), ChatType.GAME_INFO, UUID.randomUUID())
            linker.tag?.putBoolean("foil", true)
            return true
        } else {
            player.sendMessage(
                TextComponent("No viable path to input block!"),

                ChatType.GAME_INFO,
                UUID.randomUUID()
            )
        }
        return false
    }


    /**
     * This will walk the incoming links to find the root link and see if it's contained in the spark blocks
     * @return true if we have a indirect connection to a root node.
     *
     * Recursively walks backwards up the node graph to find the root nodes
     */
    private fun linkedFromRoot(): Boolean {
        val lvl = level ?: return false
        if (this.isRoot) return true
        incomingLinks.forEach<SparkTile>(lvl) {
            if (it.ctrl.isAbsent && this.ctrl.isPresent) it.ctrlPos = (this.ctrlPos)
            if (it.linkedFromRoot()) return true
        }
        return false
    }

    /**
     * Do nbt saving here
     */
    override fun onSave(tag: CompoundTag) {
        tag.put("links", outgoingLinks.serializeNBT())
        tag.put("inlinks", incomingLinks.serializeNBT())
    }

    /**
     * Load nbt data here
     */
    override fun onLoad(tag: CompoundTag) {
        outgoingLinks.deserializeNBT(tag.getCompound("links"))
        incomingLinks.deserializeNBT(tag.getCompound("inlinks"))
    }
}