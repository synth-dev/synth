package com.github.sieves.recipes

import com.github.sieves.recipes.SolidifierRecipe.Serializer.FluidIngredient
import com.github.sieves.recipes.SolidifierRecipe.Serializer.FluidIngredient.*
import com.github.sieves.recipes.internal.*
import com.github.sieves.registry.Registry.RecipeTypes.Solidifier
import com.github.sieves.registry.Registry.Recipes
import com.google.gson.*
import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.resources.*
import net.minecraft.tags.*
import net.minecraft.util.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.item.crafting.*
import net.minecraft.world.level.*
import net.minecraft.world.level.material.*
import net.minecraftforge.fluids.*
import net.minecraftforge.registries.*

class SolidifierRecipe(
    private val id: ResourceLocation,
    val fluidIngredients: NonNullList<FluidIngredient>,
    val power: Int,
    val result: ItemStack,
    val time: Int
) : ApiRecipe {

    override fun matches(inv: Container, pLevel: Level): Boolean {
        return false
    }

    override fun assemble(pContainer: Container): ItemStack {
        return ItemStack.EMPTY
    }

    override fun getResultItem(): ItemStack {
        return ItemStack.EMPTY
    }

    override fun getId(): ResourceLocation = id

    override fun getType(): RecipeType<*> {
        return Solidifier
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return Recipes.SolidifierSerializer
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

    object Serializer : ForgeRegistryEntry<RecipeSerializer<*>>(), RecipeSerializer<SolidifierRecipe> {
        override fun fromJson(recipeId: ResourceLocation, json: JsonObject): SolidifierRecipe {
            val ingredients = NonNullList.withSize(2, FluidIngredient.EMPTY)
            val time = json.getAsJsonPrimitive("time")?.asInt ?: 20
            val power = json.getAsJsonPrimitive("power")?.asInt ?: 0
            GsonHelper.getAsJsonArray(json, "ingredients").forEachIndexed { index, jsonElement ->
                val fluidIngredient = parseFluidRecipe(jsonElement)
                ingredients[index] = fluidIngredient
            }
            val result = ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result"))
            return SolidifierRecipe(recipeId, ingredients, power, result, time)
        }

        /**
         * Parses out a fluid from a json object
         */
        private fun parseFluidRecipe(json: JsonElement): FluidIngredient {
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                val tag = obj.getAsJsonPrimitive("tag").asString
                val fluid = Registry.FLUID.get(ResourceLocation(tag))
                val amount = if (obj.has("amount")) obj.getAsJsonPrimitive("amount").asInt else 1000
                return FluidIngredient(fluid, amount)
            } else error("Expected json object, found primitive for fluid recipe: $json")
        }

        /**
         * A parsed out fluid ingredient
         */
        data class FluidIngredient(val tag: Fluid, val amount: Int) {
            val fluidStack: FluidStack = FluidStack(tag, amount)

            fun toNetwork(buffer: FriendlyByteBuf) {
                buffer.writeResourceLocation(tag.registryName!!)
                buffer.writeInt(amount)
            }

            companion object {
                val EMPTY = FluidIngredient(Fluids.EMPTY, 0)
                fun fromNetwork(buffer: FriendlyByteBuf): FluidIngredient {
                    return FluidIngredient(
                        Registry.FLUID.get(buffer.readResourceLocation()), buffer.readInt()
                    )
                }
            }
        }

        override fun fromNetwork(recipeId: ResourceLocation, buffer: FriendlyByteBuf): SolidifierRecipe {
            val ingredients = NonNullList.withSize(2, FluidIngredient.EMPTY)
            ingredients.forEachIndexed { index, _ ->
                ingredients[index] = FluidIngredient.fromNetwork(buffer)
            }
            val time = buffer.readInt()
            val power = buffer.readInt()
            val item = buffer.readItem()
            return SolidifierRecipe(recipeId, ingredients, time, item, power)
        }

        override fun toNetwork(buffer: FriendlyByteBuf, recipe: SolidifierRecipe) {
            recipe.fluidIngredients.forEach {
                it.toNetwork(buffer)
            }
            buffer.writeInt(recipe.time)
            buffer.writeInt(recipe.power)
            buffer.writeItem(recipe.result)
        }


    }
}