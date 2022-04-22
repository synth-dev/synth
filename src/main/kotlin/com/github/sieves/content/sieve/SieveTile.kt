//package com.github.sieves.content.sieve
//
//import com.github.sieves.Sieves
//import com.github.sieves.content.tile.internal.Configuration
//import com.github.sieves.registry.Registry
//import net.minecraft.core.BlockPos
//import net.minecraft.core.Direction
//import net.minecraft.nbt.CompoundTag
//import net.minecraft.network.Connection
//import net.minecraft.network.chat.Component
//import net.minecraft.network.chat.TranslatableComponent
//import net.minecraft.network.protocol.Packet
//import net.minecraft.network.protocol.game.ClientGamePacketListener
//import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
//import net.minecraft.server.level.ServerPlayer
//import net.minecraft.world.Nameable
//import net.minecraft.world.SimpleMenuProvider
//import net.minecraft.world.item.ItemStack
//import net.minecraft.world.level.Level
//import net.minecraft.world.level.block.entity.BlockEntity
//import net.minecraft.world.level.block.entity.BlockEntityTicker
//import net.minecraft.world.level.block.state.BlockState
//import net.minecraftforge.common.capabilities.Capability
//import net.minecraftforge.common.util.LazyOptional
//import net.minecraftforge.energy.CapabilityEnergy
//import net.minecraftforge.energy.EnergyStorage
//import net.minecraftforge.items.CapabilityItemHandler
//import net.minecraftforge.items.ItemHandlerHelper
//import net.minecraftforge.items.ItemStackHandler
//import net.minecraftforge.network.NetworkHooks
//import kotlin.math.roundToInt
//
//class SieveTile(pos: BlockPos, state: BlockState) : BlockEntity(Registry.Tiles.Sieve, pos, state),
//    Nameable {
//    private var isCrafting = false
//    var percent = 0
//    var progress = 0
//    var energy = 0
//    var targetProgress = 20
//    var targetEnergy = 0
//    private var requiresUpdate = false
//    private var targetDamage = 1
//    var targetResult = ItemStack.EMPTY
//    private var tick = 0
//    private val threshold = 8
//    val inputInv = createInventory(2, false)
//    private val inputHandler = LazyOptional.of { inputInv }
//    val outputInv = createInventory(1, true)
//    private val outputHandler = LazyOptional.of { outputInv }
//    val energyStore = createEnergy()
//    private val energyHandler = LazyOptional.of { energyStore }
//    val config = Configuration {
//        update()
//    }
//
//    private fun createEnergy(): EnergyStorage {
//        return object : EnergyStorage(100_000) {
//            override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
//                requiresUpdate = true
//                return super.receiveEnergy(maxReceive, simulate)
//            }
//
//            override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
//                val energy = super.extractEnergy(maxExtract, simulate)
//                requiresUpdate = true
//                return energy
//            }
//        }
//    }
//
//    private fun createInventory(size: Int, output: Boolean): ItemStackHandler {
//        return object : ItemStackHandler(size) {
//            override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
//                update()
//                craft()
//                return super.extractItem(slot, amount, simulate)
//            }
//
//            override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
//                return isItemStackValid(slot, stack, output)
//            }
//
//            override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
//                update()
//                craft()
//                return super.insertItem(slot, stack, simulate)
//            }
//        }
//    }
//
//    fun getRelative(side: Configuration.Side): Direction {
//        val front = blockState.getValue(SieveBlock.FACING)
//        return when (side) {
//            Configuration.Side.Top -> Direction.UP
//            Configuration.Side.Bottom -> Direction.DOWN
//            Configuration.Side.Front -> {
//                front
//            }
//            Configuration.Side.Back -> {
//                front.opposite
//            }
//            Configuration.Side.Left -> {
//                front.opposite.counterClockWise
//            }
//            Configuration.Side.Right -> {
//                front.opposite.clockWise
//            }
//        }
//    }
//
//
//    fun update() {
//        requestModelDataUpdate()
//        setChanged()
//        if (level != null) {
//            level!!.setBlockAndUpdate(worldPosition, blockState)
//            level!!.sendBlockUpdated(worldPosition, blockState, blockState, 3)
//        }
//    }
//
//    fun itemextractItem(slot: Int, output: Boolean = false): ItemStack {
//        val count = getItemInSlot(slot).count
//        requiresUpdate = true
//        return (if (output) outputHandler else inputHandler).map { inv: ItemStackHandler ->
//            inv.extractItem(
//                slot, count, false
//            )
//        }.orElse(ItemStack.EMPTY)
//    }
//
//    fun insertItem(slot: Int, stack: ItemStack, output: Boolean = false): ItemStack {
//        val copy = stack.copy()
//        stack.shrink(copy.count)
//        requiresUpdate = true
//        return (if (output) outputHandler else inputHandler).map { inv: ItemStackHandler ->
//            inv.insertItem(
//                slot, copy, false
//            )
//        }.orElse(ItemStack.EMPTY)
//    }
//
//
//    override fun saveAdditional(tag: CompoundTag) {
//        super.saveAdditional(tag)
//        tag.put("input", inputInv.serializeNBT())
//        tag.put("output", outputInv.serializeNBT())
//        tag.put("energy", energyStore.serializeNBT())
//        tag.putInt("progress", progress)
//        tag.putInt("energyval", energy)
//        tag.putInt("target", targetProgress)
//        tag.putInt("percent", percent)
//        tag.putInt("targetenergy", targetEnergy)
//        tag.putInt("targetdamage", targetDamage)
//        tag.put("result", targetResult.serializeNBT())
//        tag.put("config", config.serializeNBT())
//    }
//
//    override fun load(tag: CompoundTag) {
//        super.load(tag)
//        inputInv.deserializeNBT(tag.getCompound("input"))
//        outputInv.deserializeNBT(tag.getCompound("output"))
//        energyStore.deserializeNBT(tag.get("energy"))
//        progress = tag.getInt("progress")
//        percent = tag.getInt("percent")
//        energy = tag.getInt("energyval")
//        targetProgress = tag.getInt("target")
//        targetEnergy = tag.getInt("targetenergy")
//        targetDamage = tag.getInt("targetdamage")
//        targetResult.deserializeNBT(tag.getCompound("result"))
//        config.deserializeNBT(tag.getCompound("config"))
//    }
//
//    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
//        return ClientboundBlockEntityDataPacket.create(this)
//    }
//
//    override fun getUpdateTag(): CompoundTag {
//        return serializeNBT()
//    }
//
//    override fun invalidateCaps() {
//        super.invalidateCaps()
//        inputHandler.invalidate()
//        outputHandler.invalidate()
//        energyHandler.invalidate()
//    }
//
//
//    override fun handleUpdateTag(tag: CompoundTag) {
//        super.handleUpdateTag(tag)
//        load(tag)
//    }
//
//    override fun onDataPacket(net: Connection, pkt: ClientboundBlockEntityDataPacket) {
//        super.onDataPacket(net, pkt)
//        handleUpdateTag(pkt.tag!!)
//    }
//
//    override fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
//        if (side == null && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return outputHandler.cast()
//        else if (side == null && cap == CapabilityEnergy.ENERGY) return energyHandler.cast()
//        else if (side == null) return super.getCapability(cap, side)
//
//        return when (config[side]) {
//            Configuration.SideConfig.InputItem -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return inputHandler.cast() else LazyOptional.empty()
//            Configuration.SideConfig.InputPower -> if (CapabilityEnergy.ENERGY == cap) return energyHandler.cast() else LazyOptional.empty()
//            Configuration.SideConfig.OutputItem -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) outputHandler.cast() else LazyOptional.empty()
//            Configuration.SideConfig.OutputPower -> if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
//            Configuration.SideConfig.None -> LazyOptional.empty()
//            Configuration.SideConfig.InputOutputItems -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return inputHandler.cast() else LazyOptional.empty()
//            Configuration.SideConfig.InputOutputPower -> if (CapabilityEnergy.ENERGY == cap) return energyHandler.cast() else LazyOptional.empty()
//            Configuration.SideConfig.InputOutputAll -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return inputHandler.cast() else if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
//        }
//    }
//
//    fun getItemInSlot(slot: Int, output: Boolean = false): ItemStack {
//        return (if (output) outputHandler else inputHandler).map { inv: ItemStackHandler ->
//            inv.getStackInSlot(
//                slot
//            )
//        }.orElse(ItemStack.EMPTY)
//    }
//
//    private fun automate() {
//
//        for (key in Direction.values()) {
//            val value = config[key]
//            val be =
//                level?.getBlockEntity(blockPos.offset(key.normal))
//                    ?: continue
//            if (value == Configuration.SideConfig.OutputItem && config.autoExport) {
//                val cap = be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, key.opposite)
//                if (cap.isPresent) {
//                    val extracted = outputInv.extractItem(0, 4, false)
//                    val leftOver = ItemHandlerHelper.insertItem(cap.resolve().get(), extracted, false)
//                    outputInv.insertItem(0, leftOver, false)
//                }
//            } else if (value == Configuration.SideConfig.InputItem && config.autoImport) {
//                val cap = be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, key.opposite)
//                if (cap.isPresent) {
//                    val other = cap.resolve().get()
//                    for (slot in 0 until other.slots) {
//                        val extracted = cap.resolve().get().extractItem(slot, 4, false)
//                        val leftOver = inputInv.insertItem(0, extracted, false)
//                        if (leftOver.isEmpty && !extracted.isEmpty) break
//                        other.insertItem(slot, leftOver, false)
//                    }
//                    for (slot in 0 until other.slots) {
//                        val extracted = cap.resolve().get().extractItem(slot, 4, false)
//                        val leftOver = inputInv.insertItem(1, extracted, false)
//                        if (leftOver.isEmpty && !extracted.isEmpty) break
//                        other.insertItem(slot, leftOver, false)
//                    }
//                }
//            } else if (value == Configuration.SideConfig.InputPower && config.autoImport) {
//                val cap = be.getCapability(CapabilityEnergy.ENERGY, key.opposite)
//                if (cap.isPresent) {
//                    val other = cap.resolve().get()
//                    val extracted = other.extractEnergy(1000, false)
//                    val leftOver = energyStore.receiveEnergy(extracted, false)
//                    val rem = 1000 - leftOver
//                    other.receiveEnergy(rem, false)
//                }
//            }
//        }
//        update()
//
//    }
//
//    fun tick() {
//        if (requiresUpdate && level != null) {
//            update()
//            requiresUpdate = false
//        }
//        if (level?.isClientSide == true) return
//        tick++
//        if (tick >= threshold) {
//            if (!isCrafting) craft()
//            tick = 0
//            automate()
//        }
//        if (isCrafting && this.energyStore.extractEnergy(targetEnergy, true) == targetEnergy) {
//            progress++
//            percent = ((progress / targetProgress.toFloat()) * 100f).roundToInt()
//            val output = outputInv.getStackInSlot(0)
//            if (getItemInSlot(0).isEmpty || getItemInSlot(1).isEmpty || (!output.isEmpty && !output.sameItem(
//                    targetResult
//                ) || output.count >= 64)
//            ) {
//                isCrafting = false
//                progress = 0
//                percent = 0
//                percent = 0
//                targetProgress = 20
//                targetDamage = 1
//                targetResult = ItemStack.EMPTY
//                update()
//            }
//            this.energyStore.extractEnergy(targetEnergy, false)
//        }
//        energy = ((this.energyStore.energyStored / this.energyStore.maxEnergyStored.toFloat()) * 100f).roundToInt()
//        if (progress >= targetProgress && isCrafting) {
//            //
////            //Finish craft here
////            val items = extractItem(0)
////            items.shrink(1)
////            insertItem(0, items)
////            val tool = getItemInSlot(1)
////            tool.damageValue = tool.damageValue + targetDamage
////            if (tool.damageValue >= tool.maxDamage) {
////                extractItem(1)
////            }
////            insertItem(0, targetResult.copy(), true)
////            isCrafting = false
////            progress = 0
////            update()
////            percent = 0
////            targetProgress = 20
////            targetDamage = 1
////            targetResult = ItemStack.EMPTY
//        }
//    }
//
//    fun openMenu(player: ServerPlayer) {
//        val menu = SimpleMenuProvider(SieveContainer.getServerContainer(this, blockPos), name)
//        NetworkHooks.openGui(player, menu, blockPos)
//    }
//
//    private fun isItemStackValid(slot: Int, input: ItemStack, output: Boolean): Boolean {
//        val recipes = level?.recipeManager?.getAllRecipesFor(Registry.RecipeTypes.Sieve) ?: return false
//        for (recipe in recipes) {
//            if (output && recipe.result.sameItem(input)) return true
//            if (recipe.ingredients[slot].test(input)) return true
//        }
//        return false
//    }
//
//    private fun craft() {
////        getRecipeFor(Registry.RecipeTypes.Sieve, container, level!!)
//        val recipes = level?.recipeManager?.getAllRecipesFor(Registry.RecipeTypes.Sieve) ?: return
//        val input = inputInv.getStackInSlot(0)
//        val tool = inputInv.getStackInSlot(1)
//
//        for (recipe in recipes) {
//            if (recipe.ingredients[0].test(input) && recipe.ingredients[1].test(tool)) {
//
//                //Valid craft!
////                tool.damageValue = tool.damageValue - it.durability
//                targetResult = recipe.result
//                targetDamage = recipe.durability
//                targetProgress = recipe.time
//                targetEnergy = recipe.power
//                isCrafting = true
//                break
//            }
//        }
//    }
//
//
//    object Ticker : BlockEntityTicker<SieveTile> {
//        override fun tick(pLevel: Level, pPos: BlockPos, pState: BlockState, pBlockEntity: SieveTile) {
//            pBlockEntity.tick()
//        }
//
//    }
//
//    override fun getName(): Component {
//        return TranslatableComponent("container.${Sieves.ModId}.sieve")
//    }
//}