package com.github.sieves.api.tile

/**
 * Used to sync handlers for example it should update the player whenever the inventory handler is changed
 */
interface IDelegateHandler {
    /**
     * Should be set to unit by default
     */
    val update: () -> Unit

}