package com.github.sieves.compat.jei

import com.github.sieves.recipes.*
import com.github.sieves.registry.*
import com.github.sieves.util.*
import com.mojang.blaze3d.vertex.*
import mezz.jei.api.constants.*
import mezz.jei.api.gui.builder.*
import mezz.jei.api.gui.drawable.*
import mezz.jei.api.helpers.*
import mezz.jei.api.ingredients.*
import mezz.jei.api.recipe.*
import mezz.jei.api.recipe.RecipeIngredientRole.*
import mezz.jei.api.recipe.category.*
import net.minecraft.client.*
import net.minecraft.client.gui.GuiComponent.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import java.text.*

//import (mezz.jei.api.gui.ingredient)

class TrashRecipeCategory(private val helper: IGuiHelper) : IRecipeCategory<SieveRecipe> {
    private val uid = "sieve".resLoc
    private val texture = "textures/gui/trash_gui.png".resLoc
    private val background = helper.createDrawable(texture, 0, 0, 176, 80)
    private val icon = helper.createDrawableIngredient(ItemStack(Registry.Blocks.Synthesizer))
    private var delta = 0f

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

        delta += Minecraft.getInstance().deltaFrameTime
        delta %= 68
        blit(stack, 165, 8, 0, 144f, 68f, 3, delta.toInt(), 256, 256)

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
        //        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 35)
//            .addIngredients(Ingredient.of(recipe.result))
//            .setSlotName("output")
    }

    override fun getRecipeClass(): Class<out SieveRecipe> {
        return SieveRecipe::class.java
    }
}