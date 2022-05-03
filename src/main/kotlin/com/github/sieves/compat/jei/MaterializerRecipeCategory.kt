package com.github.sieves.compat.jei

import com.github.sieves.recipes.*
import com.github.sieves.registry.Registry
import com.github.sieves.dsl.res
import com.mojang.blaze3d.vertex.PoseStack
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import java.text.NumberFormat

//import (mezz.jei.api.gui.ingredient)

class MaterializerRecipeCategory(private val helper: IGuiHelper) : IRecipeCategory<MaterializerRecipe> {
    private val uid = "materializer".res
    private val texture = "textures/gui/materializer_gui.png".res
    private val widgets = "textures/gui/widgets.png".res
    private val background = helper.createDrawable(texture, 0, 0, 176, 80)
    private val icon = helper.createDrawableIngredient(ItemStack(Registry.Blocks.Synthesizer))

    override fun getTitle(): Component {
        return Registry.Blocks.Materializer.name
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

    override fun setIngredients(recipe: MaterializerRecipe, ingredients: IIngredients) {
        ingredients.setInputIngredients(recipe.ingredients)
        val outputs: MutableList<MutableList<ItemStack>> = ArrayList()
        val list = ArrayList<ItemStack>()
        recipe.results.forEach { (t, u) ->
            list.add(t.items.random())
        }
        outputs.add(list)
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs)
    }

    override fun draw(recipe: MaterializerRecipe, stack: PoseStack, mouseX: Double, mouseY: Double) {
        Minecraft.getInstance().font.draw(
            stack,
            "power: ${NumberFormat.getIntegerInstance().format(recipe.power)}FE/t, ",
            5f,
            61f,
            0x404040
        )
        Minecraft.getInstance().font.draw(
            stack,
            "total power: ${NumberFormat.getIntegerInstance().format(recipe.power * recipe.time)}FE",
            5f,
            70f,
            0x404040
        )
        Minecraft.getInstance().font.draw(
            stack,
            "ticks: ${NumberFormat.getIntegerInstance().format(recipe.time)} (${recipe.time / 20}s)",
            5f,
            4f,
            0x404040
        )
    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: MaterializerRecipe, focuses: IFocusGroup) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 34)
            .addIngredients(recipe.ingredients[0])
            .setSlotName("input")
        var y = 25
        for ((index, sorted) in recipe.sortedOutput.withIndex()) {
            if (index > 4) {
                y += 18
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, ((index % 5) * 18) + 62, y)
                .addIngredients(sorted.ingredient)
                .setOverlay(ChanceDrawable(sorted.delta), 0, 0)
                .setSlotName("output_$index")
            y = 25
        }

    }

    private class ChanceDrawable(private val percent: Float) : IDrawable {
        override fun getWidth(): Int = 18

        override fun getHeight(): Int = 18

        override fun draw(poseStack: PoseStack, xOffset: Int, yOffset: Int) {
            poseStack.pushPose()
            poseStack.scale(0.45f, 0.45f, 0.45f)
            Minecraft.getInstance().font.drawShadow(
                poseStack,
                "${String.format("%.2f", percent * 100)}%",
                xOffset.toFloat(),
                yOffset.toFloat(),
                0xffffff
            )
            poseStack.popPose()
        }

    }

    override fun getRecipeClass(): Class<out MaterializerRecipe> {
        return MaterializerRecipe::class.java
    }
}