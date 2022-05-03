package com.github.sieves.content.machines.materializer

import com.github.sieves.registry.Registry
import com.github.sieves.dsl.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraftforge.common.util.INBTSerializable

/**
 * Stores the information for a materializer craft used for the tile entity
 */
data class MaterializerCraft(
    var time: Int,
    var power: Int,
    var input: ItemStack,
    var output: MutableList<ItemStack>,
    var currentTime: Int = 0
) :
    INBTSerializable<CompoundTag> {
    val isEmpty: Boolean
        get() = this == Empty

    companion object {
        val Empty get() = MaterializerCraft(0, 0, ItemStack.EMPTY, mutableListOf(), 0)
    }

    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        tag.putInt("time", time)
        tag.putInt("power", power)
        tag.put("input", input.serializeNBT())
        tag.put("output", output.toCompound())
        tag.putInt("current", currentTime)
        return tag
    }

    override fun deserializeNBT(nbt: CompoundTag) {
        time = nbt.getInt("time")
        power = nbt.getInt("power")
        input = ItemStack.of(nbt.getCompound("input"))
        output.clear()
        output.addAll(nbt.getCompound("output").toList())
        currentTime = nbt.getInt("current")
    }
}

/**
 * Handles crafting for the materializer
 */
object MaterializerCraftManager {
    /**
     * This will attempt to create a recipe from the given input
     */
    fun findRecipe(recipeManager: RecipeManager, input: ItemStack): MaterializerCraft {
        val recipes = recipeManager.getAllRecipesFor(Registry.RecipeTypes.Materializer)
        for (recipe in recipes) {
            if (recipe.ingredients[0].test(input)) {
                val outputs: ArrayList<ItemStack> = ArrayList()
                val rand = Math.random()
                for (output in recipe.sortedOutput) {
                    if (rand >= output.min && rand < output.max) {
                        outputs.add(output.ingredient.items.first())
                    }
                }
                return MaterializerCraft(recipe.time, recipe.power, input, outputs)
            }
        }
        return MaterializerCraft.Empty
    }


}