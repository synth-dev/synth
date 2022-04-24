package com.github.sieves.recipes.internal

import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe

interface ApiRecipe : Recipe<Container> {



    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return true
    }

    override fun isSpecial(): Boolean {
        return true
    }
}