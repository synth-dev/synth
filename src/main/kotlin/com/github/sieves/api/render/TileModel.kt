package com.github.sieves.api.render

import com.github.sieves.dsl.*
import net.minecraft.*
import net.minecraft.resources.*
import net.minecraft.world.level.block.entity.*
import software.bernie.geckolib3.core.*
import software.bernie.geckolib3.model.*
import kotlin.reflect.*

/**
 * Dynamically create a model based upon the input class
 */
class TileModel<T> private constructor(
    //The tile entity class used to compute the resource name. Tile entity class name must match json file names.
    //the class name can end with tile and have proper capitalization and no underscores. The class's name will be transformed.
    tileClass: KClass<T>,
    //an offset to the model's folder starting from synth:geo/.   Must start and end with a slash!!!
    modelFolder: String = "/",
    //an offset to the texture's folder starting from 'synth:textures/block/'. Must start and end with a slash!!!
    textureFolder: String = "/",
    //an offset to the animation's folder starting from synth:animations/. Must start and end with a slash!!!
    animationsFolder: String = "/"
) : AnimatedGeoModel<T>() where T : BlockEntity, T : IAnimatable {
    private var model: Opt<ResourceLocation> = nil()
    private var texture: Opt<ResourceLocation> = nil()
    private var animation: Opt<ResourceLocation> = nil()

    init {
        val resourceName = computeName(tileClass)
        val modelPath = "geo${modelFolder}${resourceName}.geo.json"
        if (ResourceLocation.isValidResourceLocation(modelPath)) model = opt(modelPath.res)
        val texturePath = "textures/block${textureFolder}${resourceName}.png"
        if (ResourceLocation.isValidResourceLocation(texturePath)) texture = opt(texturePath.res)
        val animationPath = "animations${animationsFolder}${resourceName}.animation.json"
        if (ResourceLocation.isValidResourceLocation(animationPath)) animation = opt(animationPath.res)
    }

    override fun getModelLocation(`object`: T): ResourceLocation {
        return model getOrElse { throw ResourceLocationException("Invalid model resource path!") }
    }

    override fun getTextureLocation(`object`: T): ResourceLocation {
        return texture getOrElse { throw ResourceLocationException("Invalid model resource path!") }
    }

    /**
     * This resource location needs to point to a json file of your animation file,
     * i.e. "geckolib:animations/frog_animation.json"
     *
     * @return the animation file location
     */
    override fun getAnimationFileLocation(animatable: T): ResourceLocation {
        return animation getOrElse { throw ResourceLocationException("Invalid model resource path!") }
    }

    companion object {

        internal infix fun <T> Companion.of(`class`: KClass<T>): TileModel<T> where T : BlockEntity, T : IAnimatable = TileModel(`class`)

        /**
         * MUST PASS ALL FOLDERS EVEN IF EMPTY
         */
        internal inline infix fun <reified T> Companion.of(folders: Array<String>): TileModel<T> where T : BlockEntity, T : IAnimatable =
            of(folders[0], folders[1], folders[2])

        internal inline fun <reified T> Companion.of(
            //an offset to the model's folder starting from synth:geo/.  Do not provide a leading or trailing slash.
            modelFolder: String = "",
            //an offset to the texture's folder starting from 'synth:textures/block/'. Do not provide a leading or trailing slash.
            textureFolder: String = "",
            //an offset to the animation's folder starting from synth:animations/. Do not provide a leading or trailing slash.
            animationsFolder: String = ""
        ): TileModel<T> where T : BlockEntity, T : IAnimatable = TileModel(T::class, modelFolder, textureFolder, animationsFolder)
    }

}





