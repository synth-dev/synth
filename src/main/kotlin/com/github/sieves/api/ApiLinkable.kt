package com.github.sieves.api

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

interface ApiLinkable {
    fun linkTo(pos: BlockPos, face: Direction)
    fun unlink()
}