package com.github.sieves.content.api

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.level.block.entity.BlockEntity

abstract class ApiContainer<E : BlockEntity, T : ApiContainer<E, T>>(
    protected val menuType: MenuType<T>,
    protected val id: Int,
    protected val inventory: Inventory,
    val pos: BlockPos = BlockPos.ZERO,
    val tile: E,
    protected val data: ContainerData = SimpleContainerData(1),
) : AbstractContainerMenu(menuType, id) {
    private val containerAccess = ContainerLevelAccess.create(inventory.player.level, pos)

    init {
        setupContainer()
        addDataSlots(data)
    }

    @Suppress("UNCHECKED_CAST")
    constructor(menuType: MenuType<T>, id: Int, inventory: Inventory, pos: BlockPos) : this(
        menuType,
        id,
        inventory,
        pos,
        inventory.player.level.getBlockEntity(pos) as E
    )


    /**
     * Adds the default slots for the player inventory
     */
    protected fun addDefaultSlots() {
        val size = 18
        val startX = 8
        val startY = 84
        val hotbarY = 142
        for (column in 0 until 9) {
            for (row in 0 until 3) {
                addSlot(Slot(inventory, 9 + row * 9 + column, startX + column * size, startY + row * size))
            }
            addSlot(Slot(inventory, column, startX + column * size, hotbarY))
        }
    }

    /**
     * Used to set up the container (add slots)
     */
    protected abstract fun setupContainer()

    override fun stillValid(pPlayer: Player): Boolean {
        return stillValid(containerAccess, pPlayer, tile.blockState.block)
    }


}