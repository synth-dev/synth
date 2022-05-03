package com.github.sieves.api.multiblock

import com.github.sieves.dsl.*
import net.minecraft.world.level.block.entity.*

interface ISlave<T : BlockEntity, R : BlockEntity> {

    /**Get and set the master instance**/
    var master: Opt<IMaster<T>>

    /**Used for interactions with the master**/
    var store: Opt<StructureStore>
}
