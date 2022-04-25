package com.github.sieves.content.modules.io

import com.github.sieves.api.caps.*
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.world.item.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*
import net.minecraftforge.items.*
import javax.annotation.*

class FilterCapability(private val itemstack: ItemStack, private var slots: Int) : ItemStackHandler(slots) {
    private val tag: CompoundTag
        get() {
            if (itemstack.tag == null) itemstack.tag = CompoundTag()
            return itemstack.tag!!
        }

    init {
        tag.putInt("size", slots)
    }

    class Provider(itemStack: ItemStack, slot: Int) : ICapabilityProvider {
        private val filter = FilterCapability(itemStack, slot)
        private val provider = LazyOptional.of { filter }

        /**
         * Retrieves the Optional handler for the capability requested on the specific side.
         * The return value **CAN** be the same for multiple faces.
         * Modders are encouraged to cache this value, using the listener capabilities of the Optional to
         * be notified if the requested capability get lost.
         *
         * @param cap The capability to check
         * @param side The Side to check from,
         * **CAN BE NULL**. Null is defined to represent 'internal' or 'self'
         * @return The requested an optional holding the requested capability.
         */
        override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
            if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return provider.cast()
            return LazyOptional.empty()
        }

    }


    /**
     * Returns the number of slots available
     *
     * @return The number of slots available
     */
    override fun getSlots(): Int {
        return slots
    }

    /**
     * Returns the ItemStack in a given slot.
     *
     * The result's stack size may be greater than the itemstack's max size.
     *
     * If the result is empty, then the slot is empty.
     *
     *
     *
     * **IMPORTANT:** This ItemStack *MUST NOT* be modified. This method is not for
     * altering an inventory's contents. Any implementers who are able to detect
     * modification through this method should throw an exception.
     *
     *
     *
     * ***SERIOUSLY: DO NOT MODIFY THE RETURNED ITEMSTACK***
     *
     *
     * @param slot Slot to query
     * @return ItemStack in given slot. Empty Itemstack if the slot is empty.
     */
    override fun getStackInSlot(slot: Int): ItemStack {
//        return
        validateSlotIndex(slot)
        return ItemStack.of(tag.getCompound("slot_$slot"))
    }

    /**
     * Sets the stack of the given slot to the itemstack
     */
    override fun setStackInSlot(slot: Int, itemStack: ItemStack) {
        tag.put("slot_$slot", itemStack.serializeNBT())
    }

    /**
     *
     *
     * Inserts an ItemStack into the given slot and return the remainder.
     * The ItemStack *should not* be modified in this function!
     *
     * Note: This behaviour is subtly different from [IFluidHandler.fill]
     *
     * @param slot     Slot to insert into.
     * @param stackIn    ItemStack to insert. This must not be modified by the item handler.
     * @param simulate If true, the insertion is only simulated
     * @return The remaining ItemStack that was not inserted (if the entire stack is accepted, then return an empty ItemStack).
     * May be the same as the input ItemStack if unchanged, otherwise a new ItemStack.
     * The returned ItemStack can be safely modified after.
     */
    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        val existing = getStackInSlot(slot)

        var limit = getStackLimit(slot, stack)
        if (!existing.isEmpty) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing)) return stack
            limit -= existing.count
        }
        if (limit <= 0) return stack
        val reachedLimit = stack.count > limit
        if (!simulate) {
            if (existing.isEmpty) {
                setStackInSlot(slot, if (reachedLimit) ItemHandlerHelper.copyStackWithSize(stack, limit) else stack)
            } else {
                existing.grow(if (reachedLimit) limit else stack.count)
            }
        }
        return if (reachedLimit) ItemHandlerHelper.copyStackWithSize(stack, stack.count - limit) else ItemStack.EMPTY
    }

    override fun setSize(size: Int) {
        this.slots = size
        tag.putInt("size", size)
    }

    /**
     * Extracts an ItemStack from the given slot.
     *
     *
     * The returned value must be empty if nothing is extracted,
     * otherwise its stack size must be less than or equal to `amount` and [ItemStack.getMaxStackSize].
     *
     *
     * @param slot     Slot to extract from.
     * @param amount   Amount to extract (may be greater than the current stack's max limit)
     * @param simulate If true, the extraction is only simulated
     * @return ItemStack extracted from the slot, must be empty if nothing can be extracted.
     * The returned ItemStack can be safely modified after, so item handlers should return a new or copied stack.
     */
    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        val existing: ItemStack = getStackInSlot(slot)
        if (existing.isEmpty) return ItemStack.EMPTY
        val toExtract = Math.min(amount, existing.maxStackSize)
        return if (existing.count <= toExtract) {
            if (!simulate) {
                setStackInSlot(slot, ItemStack.EMPTY)
                existing
            } else {
                existing.copy()
            }
        } else {
            if (!simulate) {
                setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.count - toExtract))
            }
            ItemHandlerHelper.copyStackWithSize(existing, toExtract)
        }
    }

    override fun getStackLimit(slot: Int, @Nonnull stack: ItemStack): Int {
        return Math.min(getSlotLimit(slot), stack.maxStackSize);
    }

    /**
     * Retrieves the maximum stack size allowed to exist in the given slot.
     *
     * @param slot Slot to query.
     * @return     The maximum stack size allowed in the slot.
     */
    override fun getSlotLimit(slot: Int): Int {
        return 64
    }

    override fun validateSlotIndex(slot: Int) {
        if (slot < 0 || slot >= slots) throw RuntimeException("Slot $slot not in valid range - [0,$slots)")
    }

    override fun serializeNBT(): CompoundTag {
        return tag
    }

    override fun deserializeNBT(nbt: CompoundTag?) {
        itemstack.tag = nbt
    }

    /**
     *
     *
     * This function re-implements the vanilla function [Container.canPlaceItem].
     * It should be used instead of simulated insertions in cases where the contents and state of the inventory are
     * irrelevant, mainly for the purpose of automation and logic (for instance, testing if a minecart can wait
     * to deposit its items into a full inventory, or if the items in the minecart can never be placed into the
     * inventory and should move on).
     *
     *
     *  * isItemValid is false when insertion of the item is never valid.
     *  * When isItemValid is true, no assumptions can be made and insertion must be simulated case-by-case.
     *  * The actual items in the inventory, its fullness, or any other state are **not** considered by isItemValid.
     *
     * @param slot    Slot to query for validity
     * @param stack   Stack to test with for validity
     *
     * @return true if the slot can insert the ItemStack, not considering the current state of the inventory.
     * false if the slot can never insert the ItemStack in any situation.
     */
    override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
        return true
    }


}