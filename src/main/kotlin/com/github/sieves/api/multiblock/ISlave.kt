package com.github.sieves.api.multiblock

import com.github.sieves.api.tile.*
import com.github.sieves.util.*
import net.minecraft.world.level.block.entity.BlockEntity

interface ISlave<T : BlockEntity, R : BlockEntity> : ITile<R> {
    /**Get and set the master instance**/
    var master: Opt<IMaster<T>>

    /**Used for interactions with the master**/
    var store: Opt<StructureStore>
}