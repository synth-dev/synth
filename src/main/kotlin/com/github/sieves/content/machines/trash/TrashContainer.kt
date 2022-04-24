package com.github.sieves.content.machines.trash

import com.github.sieves.content.machines.core.CoreContainer
import com.github.sieves.registry.Registry
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler


class TrashContainer(
    id: Int,
    inventory: Inventory,
    blockPos: BlockPos,
    tile: TrashTile,
) :
    com.github.sieves.api.ApiContainer<TrashTile, TrashContainer>(Registry.Containers.Trash, id, inventory, blockPos, tile) {

    constructor(id: Int, inventory: Inventory, pos: BlockPos) : this(
        id,
        inventory,
        pos,
        inventory.player.level.getBlockEntity(pos) as TrashTile
    )


    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var retStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val stack = slot.item
            retStack = stack.copy()
            if (index < 36) {
                if (!moveItemStackTo(stack, 36, this.slots.size, true))
                    return ItemStack.EMPTY
            } else if (!moveItemStackTo(stack, 0, 36, false)) return ItemStack.EMPTY
            if (stack.isEmpty || stack.count == 0) {
                slot.set(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
            if (stack.count == retStack.count) return ItemStack.EMPTY
            slot.onTake(player, stack)
        }
        return retStack
    }

    /**
     * Used to set up the container (add slots)
     */
    override fun setupContainer() {
        addDefaultSlots()
        for (column in 0 until 7) {
            for (row in 0 until 3) {
                val index =  row * 7 + column
                addSlot(SlotItemHandler(tile.items, index, 8 + (18 * column), 16 + (18 * row)))
            }
        }
        addDataSlots(data)
    }

}