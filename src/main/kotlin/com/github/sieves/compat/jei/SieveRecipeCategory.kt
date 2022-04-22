package com.github.sieves.compat.jei

import com.github.sieves.recipes.SieveRecipe
import com.github.sieves.registry.Registry
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.vertex.PoseStack
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IGuiIngredientGroup
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.ingredients.IIngredientType
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiComponent
import net.minecraft.client.gui.GuiComponent.blit
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import java.text.NumberFormat

//import (mezz.jei.api.gui.ingredient)

class SieveRecipeCategory(private val helper: IGuiHelper) : IRecipeCategory<SieveRecipe> {
    private val uid = "sieve".resLoc
    private val texture = "textures/gui/sieve_gui.png".resLoc
    private val background = helper.createDrawable(texture, 0, 0, 176, 80)
    private val icon = helper.createDrawableIngredient(ItemStack(Registry.Blocks.Synthesizer))

    override fun getTitle(): Component {
        return Registry.Blocks.Synthesizer.name
    }

    override fun getBackground(): IDrawable {
        return background
    }

    override fun getIcon(): IDrawable {
        return icon
    }

    override fun getUid(): ResourceLocation {
        return uid
    }

    override fun setIngredients(recipe: SieveRecipe, ingredients: IIngredients) {
        ingredients.setInputIngredients(recipe.ingredients)
        val outputs: MutableList<MutableList<ItemStack>> = ArrayList()
        outputs.add(arrayListOf(recipe.result))
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs)
    }

    override fun draw(recipe: SieveRecipe, stack: PoseStack, mouseX: Double, mouseY: Double) {
        Minecraft.getInstance().font.draw(
            stack,
            "power: ${NumberFormat.getIntegerInstance().format(recipe.power)}FE/t, ",
            5f,
            61f,
            0x404040
        )
        Minecraft.getInstance().font.draw(
            stack,
            "total power: ${NumberFormat.getIntegerInstance().format(recipe.power * recipe.time)}rf",
            5f,
            70f,
            0x404040
        )
        Minecraft.getInstance().font.draw(stack, "durability: ${recipe.durability}", 5f, 13f, 0x404040)

        Minecraft.getInstance().font.draw(
            stack,
            "ticks: ${NumberFormat.getIntegerInstance().format(recipe.time)} (${recipe.time / 20}s)",
            5f,
            4f,
            0x404040
        )

    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: SieveRecipe, focuses: IFocusGroup) {
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 33)
            .addIngredients(recipe.ingredients[0])
            .setSlotName("input")
        builder.addSlot(RecipeIngredientRole.INPUT, 66, 33)
            .addIngredients(recipe.ingredients[1])
            .setSlotName("tool")
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 35)
            .addIngredients(Ingredient.of(recipe.result))
            .setSlotName("output")
    }

    override fun getRecipeClass(): Class<out SieveRecipe> {
        return SieveRecipe::class.java
    }
}