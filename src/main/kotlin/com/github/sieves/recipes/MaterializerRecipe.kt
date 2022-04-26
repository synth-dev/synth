package com.github.sieves.recipes

import com.github.sieves.recipes.internal.ApiRecipe
import com.github.sieves.registry.Registry.RecipeTypes.Materializer
import com.github.sieves.registry.Registry.Recipes
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
import net.minecraft.world.level.Level
import net.minecraftforge.registries.ForgeRegistryEntry

class MaterializerRecipe(
    private val id: ResourceLocation,
    private val ingredients: NonNullList<Ingredient>,
    val power: Int,
    val results: Map<Ingredient, Pair<Float, Float>>,
    val time: Int
) : ApiRecipe {

    val sortedOutput: List<Output> = getSortedResults()


    override fun matches(inv: Container, pLevel: Level): Boolean {
        if (inv.containerSize < 1) return false
        return ingredients[0].test(inv.getItem(0))
    }

    override fun assemble(pContainer: Container): ItemStack {
        return ItemStack.EMPTY
    }

    override fun getResultItem(): ItemStack {
        return ItemStack.EMPTY
    }

    override fun getId(): ResourceLocation = id


    override fun getIngredients(): NonNullList<Ingredient> = ingredients


    override fun getType(): RecipeType<*> {
        return Materializer
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return Recipes.MaterializerSerializer
    }

    private fun getSortedResults(): List<Output> {
        val output = ArrayList<Output>(results.size)
        for (result in results) output.add(Output(result.key, result.value.first, result.value.second))
        output.sort()
        return output
    }

    class Output(val ingredient: Ingredient, val min: Float, val max: Float) : Comparable<Output> {
        val delta = max - min

        /**
         * Compares this object with the specified object for order. Returns zero if this object is equal
         * to the specified [other] object, a negative number if it's less than [other], or a positive number
         * if it's greater than [other].
         */
        override fun compareTo(other: Output): Int = other.delta.compareTo(delta)
    }

    object Serializer : ForgeRegistryEntry<RecipeSerializer<*>>(), RecipeSerializer<MaterializerRecipe> {
        override fun fromJson(recipeId: ResourceLocation, json: JsonObject): MaterializerRecipe {
            val ingredients = NonNullList.withSize(1, Ingredient.EMPTY)
            val time = json.getAsJsonPrimitive("time")?.asInt ?: 20
            val power = json.getAsJsonPrimitive("power")?.asInt ?: 0

            GsonHelper.getAsJsonArray(json, "ingredients").forEachIndexed { index, jsonElement ->
                val ingredient = Ingredient.fromJson(jsonElement)
                ingredients[index] = ingredient
            }
            val results = HashMap<Ingredient, Pair<Float, Float>>()
            val array = GsonHelper.getAsJsonArray(json, "results")
            array.forEachIndexed { _, json -> //Must add to one percent


                val resultItem = if (json.asJsonObject.has("item")) {
                    Ingredient.fromJson(json.asJsonObject.getAsJsonObject("item"))
                } else Ingredient.of(ItemStack.EMPTY)
                val chance = json.asJsonObject.getAsJsonObject("chance")
                val min = chance.getAsJsonPrimitive("min").asFloat
                val max = chance.getAsJsonPrimitive("max").asFloat
                results[resultItem] = min to max
            }
            return MaterializerRecipe(recipeId, ingredients, power, results, time)
        }

        override fun fromNetwork(recipeId: ResourceLocation, buffer: FriendlyByteBuf): MaterializerRecipe {
            val ingredients = NonNullList.withSize(1, Ingredient.EMPTY)
            ingredients.forEachIndexed { index, _ ->
                ingredients[index] = Ingredient.fromNetwork(buffer)
            }
            val time = buffer.readInt()
            val power = buffer.readInt()
            val resultsSize = buffer.readInt()
            val results = HashMap<Ingredient, Pair<Float, Float>>()
            for (i in 0 until resultsSize) {
                results[Ingredient.fromNetwork(buffer)] = buffer.readFloat() to buffer.readFloat()
            }
            return MaterializerRecipe(recipeId, ingredients, time, results, power)

        }

        override fun toNetwork(buffer: FriendlyByteBuf, recipe: MaterializerRecipe) {
            recipe.ingredients.forEach {
                it.toNetwork(buffer)
            }
            buffer.writeInt(recipe.time)
            buffer.writeInt(recipe.power)
            buffer.writeInt(recipe.results.size)
            for (key in recipe.results.keys) {
                val chance = recipe.results[key]
                key.toNetwork(buffer)
                buffer.writeFloat(chance!!.first)
                buffer.writeFloat(chance.second)
            }
        }


    }
}