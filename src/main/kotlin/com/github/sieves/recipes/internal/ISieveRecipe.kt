package com.github.sieves.recipes.internal

import com.github.sieves.util.resLoc
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType

interface ISieveRecipe : Recipe<Container> {



    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return true
    }

    override fun isSpecial(): Boolean {
        return true
    }
}