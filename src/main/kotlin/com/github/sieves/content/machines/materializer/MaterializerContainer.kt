package com.github.sieves.content.machines.materializer

import com.github.sieves.content.machines.forester.*
import com.github.sieves.registry.Registry
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler


class MaterializerContainer(
    id: Int,
    inventory: Inventory,
    blockPos: BlockPos,
    tile: MaterializerTile,
) :
    com.github.sieves.api.ApiContainer<MaterializerTile, MaterializerContainer>(
        Registry.Containers.Materializer,
        id,
        inventory,
        blockPos,
        tile
    ) {

    constructor(id: Int, inventory: Inventory, pos: BlockPos) : this(
        id,
        inventory,
        pos,
        inventory.player.level.getBlockEntity(pos) as MaterializerTile
    )


    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var retStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val stack = slot.item
            retStack = stack.copy()
            if (index < 36) {
                if (!moveItemStackTo(stack, 36, 37, true))
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
        addSlot(SlotItemHandler(tile.items, 0, 8, 34))
        for (column in 0 until 5) {
            for (row in 0 until 2) {
                val index = row * 5 + column
                addSlot(SlotItemHandler(tile.items, index + 1, 62 + (18 * column), 25 + (18 * row)))
            }
        }
        addDataSlots(data)
    }

}