package com.github.sieves.content.battery

import com.github.sieves.content.api.ApiContainer
import com.github.sieves.content.box.BoxTile
import com.github.sieves.registry.Registry
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler


class BoxContainer(
    id: Int,
    inventory: Inventory,
    blockPos: BlockPos,
    tile: BoxTile,
) :
    ApiContainer<BoxTile, BoxContainer>(Registry.Containers.Box, id, inventory, blockPos, tile) {

    constructor(id: Int, inventory: Inventory, pos: BlockPos) : this(
        id,
        inventory,
        pos,
        inventory.player.level.getBlockEntity(pos) as BoxTile
    )


    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var retStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val stack = slot.item
            retStack = stack.copy()
            if (index < 36) {
                if (!moveItemStackTo(stack, 36, 36 + 24, true)) return ItemStack.EMPTY
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
        val startX = 8
        val startY = 18
        for (row in 0 until 8) {
            for (column in 0 until 3) {
                val index = (column * 8 + row)
                addSlot(SlotItemHandler(tile.items, index, startX + row * 18, startY + column * 18))
            }
        }
        addDataSlots(data)
    }

}