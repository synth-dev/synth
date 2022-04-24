package com.github.sieves.recipes

import com.github.sieves.recipes.internal.*
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.RecipeTypes.Synthesizer
import com.github.sieves.registry.Registry.RecipeTypes.Trash
import com.github.sieves.registry.Registry.Recipes
import com.google.gson.*
import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.resources.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.item.crafting.*
import net.minecraft.world.level.*
import net.minecraftforge.registries.*

class TrashRecipe(
    private val id: ResourceLocation,
    private val ingredient: Ingredient,
    private val probability: Float,
    private val powerGenerated: Int
) : ApiRecipe {

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

    override fun getId(): ResourceLocation {
        return id
    }

    override fun getIngredients(): NonNullList<Ingredient> {
        return NonNullList.of(ingredient)
    }

    override fun getType(): RecipeType<*> {
        return Trash
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return Recipes.SieveSerializer
    }

    object Serializer : ForgeRegistryEntry<RecipeSerializer<*>>(), RecipeSerializer<TrashRecipe> {
        override fun fromJson(recipeId: ResourceLocation, json: JsonObject): TrashRecipe {
            val probability = json.getAsJsonPrimitive("chance").asFloat
            val ingredient = Ingredient.fromJson(json.getAsJsonObject("ingredient"))
            val powerGenerated = json.getAsJsonPrimitive("result").asInt
            return TrashRecipe(recipeId, ingredient, probability, powerGenerated)
        }

        override fun fromNetwork(recipeId: ResourceLocation, buffer: FriendlyByteBuf): TrashRecipe {
            val ingredient = Ingredient.fromNetwork(buffer)
            val chance = buffer.readFloat()
            val power = buffer.readInt()
            return TrashRecipe(recipeId, ingredient, chance, power)
        }

        override fun toNetwork(buffer: FriendlyByteBuf, recipe: TrashRecipe) {
            recipe.ingredient.toNetwork(buffer)
            buffer.writeFloat(recipe.probability)
            buffer.writeInt(recipe.powerGenerated)
        }

    }
}