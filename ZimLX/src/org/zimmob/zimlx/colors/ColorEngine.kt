/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.zimmob.zimlx.colors

import android.content.Context
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.android.launcher3.R
import com.android.launcher3.Utilities
import org.zimmob.zimlx.ZimPreferences
import org.zimmob.zimlx.ensureOnMainThread
import org.zimmob.zimlx.runOnMainThread
import org.zimmob.zimlx.useApplicationContext
import org.zimmob.zimlx.util.SingletonHolder
import org.zimmob.zimlx.util.ZimFlags
import java.util.*

class ColorEngine private constructor(val context: Context) : ZimPreferences.OnPreferenceChangeListener {

    private val prefs by lazy { Utilities.getZimPrefs(context) }
    private val colorListeners = mutableMapOf<String, MutableSet<OnColorChangeListener>>()

    private val resolverMap = mutableMapOf<String, ZimPreferences.StringBasedPref<ColorResolver>>()
    private val resolverCache = mutableMapOf<String, ColorResolver>()

    private var _accentResolver by getOrCreateResolver(Resolvers.ACCENT)
    val accentResolver get() = _accentResolver
    val accent get() = accentResolver.resolveColor()
    val accentForeground get() = accentResolver.computeForegroundColor()

    override fun onValueChanged(key: String, prefs: ZimPreferences, force: Boolean) {
        if (!force) {
            val resolver by getOrCreateResolver(key)
            resolver.startListening()
            onColorChanged(key, getOrCreateResolver(key).onGetValue())
        }
    }

    private fun onColorChanged(key: String, colorResolver: ColorResolver) {
        runOnMainThread { colorListeners[key]?.forEach { it.onColorChange(key, colorResolver.resolveColor(), colorResolver.computeForegroundColor()) } }
    }

    fun addColorChangeListeners(listener: OnColorChangeListener, vararg keys: String) {
        if (keys.isEmpty()) {
            throw RuntimeException("At least one key is required")
        }
        for (key in keys) {
            if (colorListeners[key] == null) {
                colorListeners[key] = HashSet()
                prefs.addOnPreferenceChangeListener(this, key)
            }
            colorListeners[key]?.add(listener)
            val resolver by getOrCreateResolver(key)
            listener.onColorChange(key, resolver.resolveColor(), resolver.computeForegroundColor())
        }
    }

    fun removeColorChangeListeners(listener: OnColorChangeListener, vararg keys: String) {
        if (keys.isEmpty()) {
            throw RuntimeException("At least one key is required")
        }
        for (key in keys) {
            colorListeners[key]?.remove(listener)
            if (colorListeners[key]?.isEmpty() != false) {
                prefs.removeOnPreferenceChangeListener(key, this)
            }
        }
    }

    private fun createResolverPref(key: String, defaultValue: ColorResolver = Resolvers.getDefaultResolver(key, context, this)) =
            prefs.StringBasedPref(key, defaultValue, prefs.doNothing, { string -> createColorResolver(key, string) },
                    ColorResolver::toString, ColorResolver::onDestroy)

    fun createColorResolverNullable(key: String, string: String): ColorResolver? {
        var resolver: ColorResolver? = null
        try {
            val parts = string.split("|")
            val className = parts[0]
            val args = Utilities.getZimPrefs(context).accentColor//if (parts.size > 1) parts.subList(1, parts.size) else emptyList()

            val clazz = Class.forName(className)
            val constructor = clazz.getConstructor(ColorResolver.Config::class.java)
            resolver = constructor.newInstance(ColorResolver.Config(key, this, ::onColorChanged, args)) as ColorResolver
        } catch (e: IllegalStateException) {
        } catch (e: ClassNotFoundException) {
        } catch (e: InstantiationException) {
        }
        return resolver
    }

    fun createColorResolver(key: String, string: String): ColorResolver {
        val cacheKey = "$key@$string"
        // Prevent having to expensively use reflection every time
        if (resolverCache.containsKey(cacheKey)) return resolverCache[cacheKey]!!
        val resolver = createColorResolverNullable(key, string)
        return (resolver ?: Resolvers.getDefaultResolver(key, context, this)).also {
            resolverCache[cacheKey] = it
        }
    }

    fun getOrCreateResolver(key: String, defaultValue: ColorResolver = Resolvers.getDefaultResolver(key, context, this)): ZimPreferences.StringBasedPref<ColorResolver> {
        return resolverMap[key] ?: createResolverPref(key, defaultValue).also {
            resolverMap[key] = it
        }
    }

    companion object : SingletonHolder<ColorEngine, Context>(ensureOnMainThread(useApplicationContext(::ColorEngine))) {
        @JvmStatic
        override fun getInstance(arg: Context) = super.getInstance(arg)
    }

    interface OnColorChangeListener {
        fun onColorChange(resolver: String, color: Int, foregroundColor: Int)
    }

    internal class Resolvers {
        companion object {
            const val ACCENT = ZimFlags.ACCENT_COLOR
            //const val HOTSEAT_QSB_BG = "pref_hotseatQsbColorResolver"
            //const val ALLAPPS_QSB_BG = "pref_allappsQsbColorResolver"
            fun getDefaultResolver(key: String, context: Context, engine: ColorEngine): ColorResolver {
                return when (key) {
                    /*HOTSEAT_QSB_BG -> {
                        DockQsbAutoResolver(ColorResolver.Config(key, engine, engine::onColorChanged))
                    }
                    ALLAPPS_QSB_BG -> {
                        DrawerQsbAutoResolver(ColorResolver.Config(key, engine, engine::onColorChanged))
                    }*/
                    ACCENT -> engine.createColorResolver(key, context.resources.getString(R.string.config_default_color_resolver))
                    else -> engine.createColorResolver(key, context.resources.getString(R.string.config_default_color_resolver))
                }
            }
        }
    }

    abstract class ColorResolver(val config: Config) {

        private var listening = false
        val engine get() = config.engine
        val args get() = config.selectedColor
        open val isCustom = false

        abstract fun resolveColor(): Int

        abstract fun getDisplayName(): String

        override fun toString() = TextUtils.join("|", listOf(this::class.java.name) + args) as String

        open fun computeForegroundColor(): Int {
            return Palette.Swatch(ColorUtils.setAlphaComponent(resolveColor(), 0xFF), 1).bodyTextColor
        }

        open fun startListening() {
            listening = true
        }

        open fun stopListening() {
            listening = false
        }

        fun notifyChanged() {
            config.listener?.invoke(config.key, this)
        }

        fun onDestroy() {
            if (listening) {
                stopListening()
            }
        }

        class Config(
                val key: String,
                val engine: ColorEngine,
                val listener: ((String, ColorResolver) -> Unit)? = null,
                val selectedColor: Int)
        //val args: List<String> = emptyList())
    }
}
