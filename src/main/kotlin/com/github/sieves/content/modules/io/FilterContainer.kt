package com.github.sieves.content.modules.io

import com.github.sieves.api.caps.*
import com.github.sieves.registry.Registry.Containers
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.*

class FilterContainer(id: Int, private val inventory: Inventory, val itemStack: ItemStack) :
    AbstractContainerMenu(Containers.Filter, id) {

    init {
        addDefaultSlots()
        val cap = itemStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        if (cap.isPresent) {
            for (column in 0 until 9) {
                for (row in 0 until 2) {
                    val index = row * 9 + column
                    addSlot(SlotItemHandler(cap.resolve().get(), index, 8 + (18 * column), 14 + (18 * row)))
                }
            }
        }

    }

    /**
     * Puts an ItemStack in a slot.
     */
    override fun setItem(pSlotId: Int, pStateId: Int, pStack: ItemStack) {
        super.setItem(pSlotId, pStateId, pStack)

    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var retStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val stack = slot.item
            retStack = stack.copy()
            if (index < 36) {
                if (!moveItemStackTo(stack, 36, this.slots.size, false))
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
     * Adds the default slots for the player inventory
     */
    private fun addDefaultSlots() {
        val size = 18
        val startX = 8
        val startY = 62
        val hotbarY = 120
        for (column in 0 until 9) {
            for (row in 0 until 3) {
                addSlot(Slot(inventory, 9 + row * 9 + column, startX + column * size, startY + row * size))
            }
            addSlot(Slot(inventory, column, startX + column * size, hotbarY))
        }
    }

    /**
     * Determines whether supplied player can use this container
     */
    override fun stillValid(pPlayer: Player): Boolean {
        return true
    }
}