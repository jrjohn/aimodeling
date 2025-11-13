package com.example.aimodel.core.common

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for providing string resources
 * Allows ViewModels and other classes to access strings without direct Context dependency
 */
interface StringProvider {
    /**
     * Gets a string resource by its ID
     *
     * @param resId The string resource ID
     * @return The localized string
     */
    fun getString(@StringRes resId: Int): String

    /**
     * Gets a formatted string resource
     *
     * @param resId The string resource ID
     * @param formatArgs Format arguments to substitute into the string
     * @return The formatted localized string
     */
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String

    /**
     * Gets a plural string resource
     *
     * @param resId The plurals resource ID
     * @param quantity The quantity to use for plural selection
     * @return The localized plural string
     */
    fun getQuantityString(@PluralsRes resId: Int, quantity: Int): String

    /**
     * Gets a formatted plural string resource
     *
     * @param resId The plurals resource ID
     * @param quantity The quantity to use for plural selection
     * @param formatArgs Format arguments to substitute into the string
     * @return The formatted localized plural string
     */
    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String
}

/**
 * Implementation of StringProvider using Android Context
 */
@Singleton
class AndroidStringProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : StringProvider {

    override fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }

    override fun getQuantityString(@PluralsRes resId: Int, quantity: Int): String {
        return context.resources.getQuantityString(resId, quantity)
    }

    override fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String {
        return context.resources.getQuantityString(resId, quantity, *formatArgs)
    }
}
