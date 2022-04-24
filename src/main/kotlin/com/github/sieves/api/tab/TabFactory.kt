package com.github.sieves.api.tab

import com.github.sieves.api.ApiTab
import java.util.UUID

/**
 * Binds a tag to an owner
 */
fun interface TabFactory {
    /**
     * Create a tab for the given uuid
     */
    fun create(uuid: UUID): ApiTab
}