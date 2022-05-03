package com.github.sieves.registry.internal.net

import com.github.sieves.api.ApiConfig
import com.github.sieves.content.machines.materializer.*
import com.github.sieves.dsl.res
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.UUID

class ConfigurePacket() : Packet() {
    var config = ApiConfig {}
    var blockPos = BlockPos.ZERO
    var world: ResourceKey<Level> = Level.OVERWORLD

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeResourceLocation(world.location())
        buffer.writeNbt(config.serializeNBT())
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        world = ResourceKey.create(Registry.DIMENSION_REGISTRY, buffer.readResourceLocation())
        buffer.readNbt()?.let { config.deserializeNBT(it) }
    }

    override fun toString(): String {
        return "ConfigurePacket(config=$config, blockPos=$blockPos, world=$world)"
    }

}

class StartMaterializer() : Packet() {
    var blockPos: BlockPos = BlockPos.ZERO
    var craft: MaterializerCraft = MaterializerCraft.Empty

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeNbt(craft.serializeNBT())
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        craft.deserializeNBT(buffer.readNbt()!!)
    }

}

class StartSolidifer() : Packet() {
    var blockPos: BlockPos = BlockPos.ZERO
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
    }

}

class StopSolidifer() : Packet() {
    var blockPos: BlockPos = BlockPos.ZERO
    var item: ItemStack = ItemStack.EMPTY
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeItem(item)
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        item = buffer.readItem()
    }

}

class StopMaterializer() : Packet() {
    var blockPos: BlockPos = BlockPos.ZERO

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
    }

}

class TabUpdatePacket() : Packet() {
    var key: ResourceLocation = "missing".res
    var tab = CompoundTag()

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(key)
        buffer.writeNbt(tab)
    }

    override fun read(buffer: FriendlyByteBuf) {
        key = buffer.readResourceLocation()
        tab = buffer.readNbt()!!
    }
}

/**
 * Delete our items from the renderer
 */
class DeleteItemPacket() : Packet() {
    var blockPos = BlockPos.ZERO
    var slot: Int = -1
    var item = ItemStack.EMPTY
    var result = ItemStack.EMPTY

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeInt(slot)
        buffer.writeItem(item)
        buffer.writeItem(result)
    }

    override fun read(buffer: FriendlyByteBuf) {
        this.blockPos = buffer.readBlockPos()
        this.slot = buffer.readInt()
        this.item = buffer.readItem()
        this.result = buffer.readItem()
    }


}

/**
 * Tells the client to play a certain sound
 */
class PlaySoundPacket() : Packet() {
    var sound: SoundEvent = SoundEvents.ENDERMAN_TELEPORT

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(sound.location)
    }

    override fun read(buffer: FriendlyByteBuf) {
        sound = Registry.SOUND_EVENT.get(buffer.readResourceLocation())!!
    }

}

/**
 * Tells the client to play a certain sound
 */
class ToggleSightPacket() : Packet() {
    var enabled: Boolean = false

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBoolean(enabled)
    }

    override fun read(buffer: FriendlyByteBuf) {
        enabled = buffer.readBoolean()
    }
}

/**
 * Tells the client to play a certain sound
 */
class ToggleStepPacket() : Packet() {
    var enabled: Boolean = false
    var uuid: UUID = UUID.randomUUID()

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBoolean(enabled)
        buffer.writeUUID(uuid)
    }

    override fun read(buffer: FriendlyByteBuf) {
        enabled = buffer.readBoolean()
        uuid = buffer.readUUID()
    }
}


/**
 * Tells the client to play a certain sound
 */
class FlightPacket() : Packet() {
    var enabled: Boolean = false
    var uuid: UUID = UUID.randomUUID()

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBoolean(enabled)
        buffer.writeUUID(uuid)
    }

    override fun read(buffer: FriendlyByteBuf) {
        enabled = buffer.readBoolean()
        uuid = buffer.readUUID()
    }
}


class MenuStatePacket() : Packet() {
    var player: UUID = UUID.randomUUID()
    var menu: ResourceLocation = "".res
    var opened: Boolean = false

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUUID(player)
        buffer.writeResourceLocation(menu)
        buffer.writeBoolean(opened)
    }

    override fun read(buffer: FriendlyByteBuf) {
        this.player = buffer.readUUID()
        this.menu = buffer.readResourceLocation()
        this.opened = buffer.readBoolean()
    }

}


class TeleportPacket() : Packet() {
    var playerUuid: UUID = UUID.randomUUID()
    var toLocation: Vec3 = Vec3.ZERO

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUUID(playerUuid)
        buffer.writeDouble(toLocation.x)
        buffer.writeDouble(toLocation.y)
        buffer.writeDouble(toLocation.z)
    }

    override fun read(buffer: FriendlyByteBuf) {
        playerUuid = buffer.readUUID()
        val x = buffer.readDouble()
        val y = buffer.readDouble()
        val z = buffer.readDouble()
        toLocation = Vec3(x, y, z)
    }

    override fun toString(): String {
        return "TeleportPacket(playerUuid=$playerUuid, toLocation=$toLocation)"
    }


}

class TabClickedPacket() : Packet() {
    var uuid: UUID = UUID.randomUUID() //lol
    var key: ResourceLocation = "missing".res


    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUUID(uuid)
        buffer.writeResourceLocation(key)
    }

    override fun read(buffer: FriendlyByteBuf) {
        uuid = buffer.readUUID()
        key = buffer.readResourceLocation()
    }

    override fun toString(): String {
        return "TabClickedPacket(uuid=$uuid, key=$key)"
    }

}

class TabBindPacket() : Packet() {
    var uuid: UUID = UUID.randomUUID() //lol
    var key: ResourceLocation = "missing".res

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUUID(uuid)
        buffer.writeResourceLocation(key)
    }

    override fun read(buffer: FriendlyByteBuf) {
        uuid = buffer.readUUID()
        key = buffer.readResourceLocation()
    }

    override fun toString(): String {
        return "TabInstantiatePacket(uuid=$uuid, key=$key)"
    }

}


class GrowBlockPacket() : Packet() {
    var blockPos: BlockPos = BlockPos.ZERO
    var ownerPos: BlockPos = BlockPos.ZERO
    var isFarmer = false

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeBlockPos(ownerPos)
        buffer.writeBoolean(isFarmer)
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        ownerPos = buffer.readBlockPos()
        isFarmer = buffer.readBoolean()
    }

    override fun toString(): String {
        return "GrowBlockPacket(blockPos=$blockPos, ownerPos=$ownerPos)"
    }


}

class TakeUpgradePacket : Packet() {
    var blockPos: BlockPos = BlockPos.ZERO
    var slot: Int = 0
    var count: Int = 0

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeInt(slot)
        buffer.writeInt(count)

    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        slot = buffer.readInt()
        count = buffer.readInt()
    }

    override fun toString(): String {
        return "TakeUpgradePacket(blockPos=$blockPos, slot=$slot, count=$count)"
    }

}


class HarvestBlockPacket() : Packet() {
    var blockPos = BlockPos.ZERO
    var ownerPos = BlockPos.ZERO
    val harvested = ArrayList<ItemStack>()
    var isFarmer = false

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeBlockPos(ownerPos)
        buffer.writeInt(harvested.size)
        harvested.forEach {
            buffer.writeItem(it)
        }
        buffer.writeBoolean(isFarmer)
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        ownerPos = buffer.readBlockPos()
        val count = buffer.readInt()
        harvested.clear()
        for (i in 0 until count) {
            harvested.add(buffer.readItem())
        }
        isFarmer = buffer.readBoolean()
    }

    override fun toString(): String {
        return "HarvestBlockPacket(blockPos=$blockPos, ownerPos=$ownerPos, harvested=$harvested)"
    }


}


