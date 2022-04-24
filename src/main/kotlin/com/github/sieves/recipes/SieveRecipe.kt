package com.github.sieves.recipes

import com.github.sieves.recipes.internal.ApiRecipe
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.RecipeTypes.Synthesizer
import com.google.gson.JsonObject
import net.minecraft.core.NonNullList
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.level.Level
import net.minecraftforge.registries.ForgeRegistryEntry

class SieveRecipe(
    private val id: ResourceLocation,
    private val ingredients: NonNullList<Ingredient>,
    val power: Int,
    val result: ItemStack,
    val time: Int,
    val durability: Int,
) : ApiRecipe {

    override fun matches(inv: Container, pLevel: Level): Boolean {
        if (inv.containerSize < 2) return false
        return ingredients[0].test(inv.getItem(0)) && ingredients[1].test(inv.getItem(1))
    }

    override fun assemble(pContainer: Container): ItemStack {
        return result
    }

    override fun getResultItem(): ItemStack {
        return result.copy()
    }

    override fun getId(): ResourceLocation {
        return id
    }

    override fun getIngredients(): NonNullList<Ingredient> {
        return ingredients
    }


    override fun getType(): RecipeType<*> {
        return Synthesizer
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return Registry.Recipes.SieveSerializer
    }

    object Serializer : ForgeRegistryEntry<RecipeSerializer<*>>(), RecipeSerializer<SieveRecipe> {
        override fun fromJson(recipeId: ResourceLocation, json: JsonObject): SieveRecipe {
            val ingredients = NonNullList.withSize(2, Ingredient.EMPTY)
            val time = json.getAsJsonPrimitive("time")?.asNumber ?: 20
            val durability = json.getAsJsonPrimitive("durability")?.asNumber ?: 1
            val power = json.getAsJsonPrimitive("power")?.asInt ?: 0

            GsonHelper.getAsJsonArray(json, "ingredients").forEachIndexed { index, jsonElement ->
                val ingredient = Ingredient.fromJson(jsonElement)
                ingredients[index] = ingredient
            }
            val result = ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result"))
            return SieveRecipe(recipeId, ingredients, power, result, time.toInt(), durability.toInt())
        }

        override fun fromNetwork(recipeId: ResourceLocation, buffer: FriendlyByteBuf): SieveRecipe {
            val ingredients = NonNullList.withSize(2, Ingredient.EMPTY)
            ingredients.forEachIndexed { index, _ ->
                ingredients[index] = Ingredient.fromNetwork(buffer)
            }
            val output = buffer.readItem()
            val power = buffer.readInt()
            val time = buffer.readInt()
            val durability = buffer.readInt()
            return SieveRecipe(recipeId, ingredients, power, output, time, durability)
        }

        override fun toNetwork(buffer: FriendlyByteBuf, recipe: SieveRecipe) {
            recipe.ingredients.forEach {
                it.toNetwork(buffer)
            }
            buffer.writeItem(recipe.result)
            buffer.writeInt(recipe.power)
            buffer.writeInt(recipe.time)
            buffer.writeInt(recipe.durability)
        }


    }
}