//package com.github.sieves.content.sieve
//
//import com.github.sieves.registry.Registry
//import net.minecraft.client.Minecraft
//import net.minecraft.core.BlockPos
//import net.minecraft.network.FriendlyByteBuf
//import net.minecraft.server.level.ServerLevel
//import net.minecraft.world.entity.player.Inventory
//import net.minecraft.world.entity.player.Player
//import net.minecraft.world.inventory.*
//import net.minecraft.world.item.ItemStack
//import net.minecraftforge.api.distmarker.Dist
//import net.minecraftforge.items.IItemHandler
//import net.minecraftforge.items.ItemStackHandler
//import net.minecraftforge.items.SlotItemHandler
//import thedarkcolour.kotlinforforge.forge.callWhenOn
//
//
//class SieveContainer(
//    val id: Int,
//    private val inventory: Inventory,
//    private val slotsIn: IItemHandler = ItemStackHandler(2),
//    private val slotsOut: IItemHandler = ItemStackHandler(1),
//    var pos: BlockPos = BlockPos.ZERO,
//    val data: ContainerData = SimpleContainerData(2),
//    val player: Player = inventory.player,
//    val buf: FriendlyByteBuf? = null
//) : AbstractContainerMenu(Registry.Containers.Sieve, id) {
//    private val containerAccess = ContainerLevelAccess.create(inventory.player.level, pos)
//    var tile: SieveTile? = null
//
//    init {
//        val size = 18
//        val startX = 8
//        val startY = 84
//        val hotbarY = 142
//        var i = 0
//        for (column in 0 until 9) {
//            for (row in 0 until 3) {
//                addSlot(Slot(inventory, 9 + row * 9 + column, startX + column * size, startY + row * size))
//                i++
//            }
//            addSlot(Slot(inventory, column, startX + column * size, hotbarY))
//            i++
//        }
//        addSlot(SlotItemHandler(slotsIn, 0, 35, 33))
//        addSlot(SlotItemHandler(slotsIn, 1, 66, 33))
//        addSlot(SlotItemHandler(slotsOut, 0, 124, 35))
//        addDataSlots(data)
//        callWhenOn(Dist.CLIENT) {
//            val tile = Minecraft.getInstance().level?.getBlockEntity(pos)
//            if (tile is SieveTile) this.tile = tile
//        }
//    }
//
//
//    override fun stillValid(pPlayer: Player): Boolean {
//        return stillValid(containerAccess, pPlayer, Registry.Blocks.Sieve)
//    }
//
//    override fun quickMoveStack(player: Player, index: Int): ItemStack {
//        var retStack = ItemStack.EMPTY
//        val slot = slots[index]
//        if (slot.hasItem()) {
//            val stack = slot.item
//            retStack = stack.copy()
//            if (index < 36) {
//                if (!moveItemStackTo(stack, 36, this.slots.size, true)) return ItemStack.EMPTY
//            } else if (!moveItemStackTo(stack, 0, 36, false)) return ItemStack.EMPTY
//            if (stack.isEmpty || stack.count == 0) {
//                slot.set(ItemStack.EMPTY)
//            } else {
//                slot.setChanged()
//            }
//            if (stack.count == retStack.count) return ItemStack.EMPTY
//            slot.onTake(player, stack)
//        }
//        return retStack
//    }
//
//    companion object {
//        fun getServerContainer(sieve: SieveTile, pos: BlockPos): MenuConstructor {
//            return MenuConstructor { id, inv, _ ->
//                SieveContainer(
//                    id,
//                    inv,
//                    sieve.inputInv,
//                    sieve.outputInv,
//                    pos,
//                    player = inv.player,
//                    data = SieveContainerData(2, sieve)
//
//                )
//            }
//        }
//
//    }
//}