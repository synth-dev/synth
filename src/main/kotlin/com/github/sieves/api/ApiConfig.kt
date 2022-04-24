package com.github.sieves.api

import com.github.sieves.registry.Registry
import com.mojang.math.Vector3f
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.items.ItemStackHandler
import java.util.*

class ApiConfig(private val update: () -> Unit) : INBTSerializable<CompoundTag> {
    private val sideConfigs: MutableMap<Direction, SideConfig> = EnumMap(Direction::class.java)

    //TODO: make this configurable
    val speedModifier: Float get() = 1.0f + (1.25f * upgrades.getStackInSlot(0).count)

    //TODO: make this configurable
    val efficiencyModifier: Float get() = 1.0f + (4.25f * upgrades.getStackInSlot(1).count)

    var autoExport = false
    var autoImport = false


    fun identityConfiguration(): ApiConfig {
        for (dir in Direction.values()) sideConfigs[dir] = SideConfig.InputOutputAll
        return this
    }

    //Stores our item upgrades
    val upgrades = object : ItemStackHandler(2) {
        override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
            if (!isItemValid(slot, stack)) return stack
            val inserted = super.insertItem(slot, stack, simulate)
            update()
            return inserted
        }

        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
            val extracted = super.extractItem(slot, amount, simulate)
            update()
            return extracted
        }

        /**
         * This only allows you to insert when there is 3 or less speed upgrades or efficiency upgrades.
         * TODO: not hard code this?
         */
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            if (slot == 0 && stack.item != Registry.Items.SpeedUpgrade) return false
            if (slot == 1 && stack.item != Registry.Items.EfficiencyUpgrade) return false
//            if (slot == 0 && getStackInSlot(0).count >= 4) return false
//            if (slot == 1 && getStackInSlot(1).count >= 4) return false
            return true
        }
    }


    operator fun set(direction: Direction, sideConfig: SideConfig) {
        sideConfigs[direction] = sideConfig
    }

    operator fun get(direction: Direction): SideConfig {
        return sideConfigs[direction] ?: SideConfig.None
    }


    enum class Side {
        Top, Bottom, Front, Back, Left, Right
    }

    enum class SideConfig(
        val displayName: String,
        color: String,
        val canImportItem: Boolean,
        val canExportItem: Boolean,
        val canImportPower: Boolean,
        val canExportPower: Boolean,
        val canImportFluid: Boolean,
        val canExportFluid: Boolean
    ) {
        InputItem(
            "Import Items",
            "#0085c3", true,
            false,
            false,
            false,
            false,
            false
        ),
        OutputItem(
            "Export Items",
            "#7ab800",
            false,
            true,
            false,
            false,
            false,
            false
        ),
        InputPower(
            "Import Power",
            "#f2af00",
            false,
            false,
            true,
            false,
            false,
            false
        ),
        OutputPower(
            "Export Power",
            "#dc5034",
            false,
            false,
            false,
            true,
            false,
            false
        ),
        InputOutputItems(
            "Import/Export Items",
            "#eeeeee",
            true,
            true,
            false,
            false,
            false,
            false
        ),
        InputOutputPower(
            "Import/Export Power",
            "#b7295a",
            false,
            false,
            true,
            true,
            false,
            false
        ),
        InputFluid(
            "Import Fluids",
            "#a8325f",
            false,
            false,
            false,
            false,
            true,
            false
        ),
        OutputFluid(
            "Export Fluids",
            "#3275a8",
            false,
            false,
            false,
            false,
            false,
            true
        ),
        InputOutputFluid(
            "Import/Export Fluids",
            "#e9ed68",
            false,
            false,
            false,
            false,
            false,
            true
        ),
        InputOutputAll(
            "Import/Export Items, Power, and Fluids",
            "#32a84e",
            true,
            true,
            true,
            true,
            true,
            true
        ),
        None(
            "None",
            "#404040",
            false,
            false,
            false,
            false,
            false,
            false
        );

        val nextIndex: Int get() = (this.ordinal + 1) % (values().size)
        val color: Vector3f = Vector3f(
            Integer.valueOf(color.substring(1, 3), 16) / 255f,
            Integer.valueOf(color.substring(3, 5), 16) / 255f,
            Integer.valueOf(color.substring(5, 7), 16) / 255f
        )
        val next: SideConfig get() = values()[nextIndex]
        val previous: SideConfig get() = values()[if (ordinal == 0) values().size - 1 else ordinal - 1]

    }

    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        var i = 0
        tag.putBoolean("autoImport", autoImport)
        tag.putBoolean("autoExtract", autoExport)
        sideConfigs.forEach { (t, u) ->
            val side = CompoundTag()
            side.putInt("key", t.ordinal)
            side.putInt("value", u.ordinal)
            tag.put("side_${i++}", side)
        }
        tag.putInt("count", i)
        tag.put("upgrades", upgrades.serializeNBT())
        return tag
    }


    override fun deserializeNBT(nbt: CompoundTag) {
        val count = nbt.getInt("count")
        this.autoImport = nbt.getBoolean("autoImport")
        this.autoExport = nbt.getBoolean("autoExtract")

        for (i in 0 until count) {
            val side = nbt.getCompound("side_${i}")
            val key = Direction.values()[side.getInt("key")]
            val value = SideConfig.values()[side.getInt("value")]
            sideConfigs[key] = value
        }
        upgrades.deserializeNBT(nbt.getCompound("upgrades"))
    }
}