package com.github.sieves.recipes.internal

import net.minecraft.core.*
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.*

interface ApiRecipe : Recipe<Container> {
    override fun getIngredients(): NonNullList<Ingredient> = NonNullList.withSize(0, Ingredient.EMPTY)


    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return true
    }

    override fun isSpecial(): Boolean {
        return true
    }
}