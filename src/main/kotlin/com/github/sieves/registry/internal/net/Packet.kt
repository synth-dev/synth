package com.github.sieves.registry.internal.net

import net.minecraft.network.FriendlyByteBuf

abstract class Packet {
    abstract fun write(buffer: FriendlyByteBuf)
    abstract fun read(buffer: FriendlyByteBuf)
}
