package com.github.sieves.content.io.fluids

import com.github.sieves.registry.Registry
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler


class FluidsContainer(
    id: Int,
    inventory: Inventory,
    blockPos: BlockPos,
    tile: FluidsTile,
) :
    com.github.sieves.api.ApiContainer<FluidsTile, FluidsContainer>(
        Registry.Containers.Fluids,
        id,
        inventory,
        blockPos,
        tile
    ) {

    constructor(id: Int, inventory: Inventory, pos: BlockPos) : this(
        id,
        inventory,
        pos,
        inventory.player.level.getBlockEntity(pos) as FluidsTile
    )


    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var retStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val stack = slot.item
            retStack = stack.copy()
            if (index < 36) {
                if (!moveItemStackTo(stack, 36, 37, true)) return ItemStack.EMPTY
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
        addSlot(SlotItemHandler(tile.items, 0, 18, 18))
        addDataSlots(data)
    }

}