package com.github.sieves.compat.jei

import com.github.sieves.recipes.*
import com.github.sieves.registry.Registry
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.systems.*
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
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.client.renderer.texture.*
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*
import net.minecraftforge.fluids.*
import java.text.NumberFormat

//import (mezz.jei.api.gui.ingredient)

class SolidifierRecipeCategory(private val helper: IGuiHelper) : IRecipeCategory<SolidifierRecipe> {
    private val uid = "solidifer".resLoc
    private val texture = "textures/gui/solidifier_gui.png".resLoc
    private val background = helper.createDrawable(texture, 0, 0, 176, 80)
    private val icon = helper.createDrawableIngredient(ItemStack(Registry.Blocks.Core))

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

    override fun setIngredients(recipe: SolidifierRecipe, ingredients: IIngredients) {
//        ingredients.setInputIngredients(recipe.ingredients)
        val outputs: MutableList<MutableList<ItemStack>> = ArrayList()
        val list = ArrayList<ItemStack>()
        list.add(recipe.result)
        outputs.add(list)
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs)
    }

    override fun draw(recipe: SolidifierRecipe, stack: PoseStack, mouseX: Double, mouseY: Double) {
        Minecraft.getInstance().font.draw(
            stack,
            "power: ${NumberFormat.getIntegerInstance().format(recipe.power)}FE/t",
            85f,
            4f,
            0x404040
        )
        Minecraft.getInstance().font.draw(
            stack,
            "total power: ${NumberFormat.getIntegerInstance().format(recipe.power * recipe.time)}FE",
            5f,
            72f,
            0x404040
        )

        Minecraft.getInstance().font.draw(
            stack,
            "ticks: ${NumberFormat.getIntegerInstance().format(recipe.time)} (${recipe.time / 20}s)",
            5f,
            4f,
            0x404040
        )

        drawFluid(
            stack, 23, 17, 24, 53, recipe.fluidIngredients[0].fluidStack, recipe.fluidIngredients[0].fluidStack.amount
        )

        drawFluid(
            stack, 73, 17, 24, 53, recipe.fluidIngredients[1].fluidStack, recipe.fluidIngredients[1].fluidStack.amount
        )

    }

    open fun drawFluid(
        poseStack: PoseStack,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fluidStack: FluidStack,
        capacityMb: Int
    ) {
        val fluid = fluidStack.fluid ?: return
        val fluidStillSprite: TextureAtlasSprite = getStillFluidSprite(fluidStack)
        val attributes = fluid.attributes
        val fluidColor = attributes.getColor(fluidStack)
        val amount = fluidStack.amount
        var scaledAmount: Int = amount * height / capacityMb
        if (amount > 0 && scaledAmount < 1) {
            scaledAmount = 10
        }
        if (scaledAmount > height) {
            scaledAmount = height
        }
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS)
        val red: Int = fluidColor and 0xff
        val green: Int = fluidColor and 0xff00 shr 8
        val blue: Int = fluidColor and 0xff0000 shr 16
        RenderSystem.setShaderColor(blue / 255f, green / 255f, red / 255f, 0.9f)
        AbstractContainerScreen.blit(poseStack, x, y, 80, width, scaledAmount, fluidStillSprite)
    }


    fun getStillFluidSprite(fluidStack: FluidStack): TextureAtlasSprite {
        val minecraft = Minecraft.getInstance()
        val fluid = fluidStack.fluid
        val attributes = fluid.attributes
        val fluidStill = attributes.getStillTexture(fluidStack)
        return minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidStill)
    }


    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: SolidifierRecipe, focuses: IFocusGroup) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 137, 35)
            .addIngredients(Ingredient.of(recipe.result))
            .setSlotName("output")
    }

    override fun getRecipeClass(): Class<out SolidifierRecipe> {
        return SolidifierRecipe::class.java
    }
}