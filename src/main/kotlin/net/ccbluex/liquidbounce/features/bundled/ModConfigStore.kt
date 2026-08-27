/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
 *
 * Tsunami is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tsunami is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tsunami. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.bundled

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/**
 * Read and write access to a bundled mod's own config file.
 *
 * Tsunami bundles mods rather than reimplementing them, and every one of them
 * arrives with its own settings screen and its own config file. Asking a player
 * to configure half their client in one UI and half in six others is the thing
 * this bridge exists to avoid: the ClickGUI is the only place anything is
 * configured, and these classes are what make that true for a mod that knows
 * nothing about us.
 *
 * ## What this deliberately does not pretend to do
 *
 * These mods read their config once, at startup, and keep it in memory. Writing
 * the file does not reach into a running mod and change its mind. So a value
 * changed here takes effect **on the next launch**, and every setting that goes
 * through this bridge says so rather than appearing to do something it did not.
 *
 * Editing in place is also why every write preserves keys we do not know about:
 * a mod update that adds an option must not have it wiped by us writing back a
 * config we only partly understand.
 */
sealed class ModConfigStore(protected val path: Path) {

    abstract fun readBoolean(key: String): Boolean?
    abstract fun readInt(key: String): Int?
    abstract fun readFloat(key: String): Float?
    abstract fun write(values: Map<String, Any>)

    protected fun exists() = Files.isRegularFile(path)

    companion object {

        private val configDir: Path get() = FabricLoader.getInstance().configDir

        /**
         * Whether a mod is actually present.
         *
         * In development none of the launcher-installed mods exist, and a
         * player can always delete one. Settings for something that is not
         * there would be settings that silently do nothing.
         */
        fun isModLoaded(modId: String): Boolean =
            FabricLoader.getInstance().isModLoaded(modId)

        fun json(fileName: String) = JsonConfigStore(configDir.resolve(fileName))

        fun properties(fileName: String) = PropertiesConfigStore(configDir.resolve(fileName))
    }
}

/**
 * A JSON config, addressed with dotted paths.
 *
 * Sodium nests its options by category (`performance.use_entity_culling`);
 * ImmediatelyFast keeps them flat. A dotted key covers both without each
 * caller caring which it is talking to.
 */
class JsonConfigStore(path: Path) : ModConfigStore(path) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private fun root(): JsonObject? {
        if (!exists()) {
            return null
        }

        return runCatching {
            JsonParser.parseString(Files.readString(path)).asJsonObject
        }.onFailure {
            logger.warn("Could not read ${path.fileName}", it)
        }.getOrNull()
    }

    private fun resolve(root: JsonObject, key: String, create: Boolean): Pair<JsonObject, String>? {
        val parts = key.split('.')
        var node = root

        for (part in parts.dropLast(1)) {
            val child = node.getAsJsonObject(part)
            if (child == null) {
                if (!create) {
                    return null
                }
                val fresh = JsonObject()
                node.add(part, fresh)
                node = fresh
            } else {
                node = child
            }
        }

        return node to parts.last()
    }

    override fun readBoolean(key: String): Boolean? {
        val root = root() ?: return null
        val (holder, leaf) = resolve(root, key, create = false) ?: return null
        val element = holder.get(leaf) ?: return null
        return runCatching { element.asBoolean }.getOrNull()
    }

    override fun readInt(key: String): Int? {
        val root = root() ?: return null
        val (holder, leaf) = resolve(root, key, create = false) ?: return null
        val element = holder.get(leaf) ?: return null
        return runCatching { element.asInt }.getOrNull()
    }

    override fun readFloat(key: String): Float? {
        val root = root() ?: return null
        val (holder, leaf) = resolve(root, key, create = false) ?: return null
        val element = holder.get(leaf) ?: return null
        return runCatching { element.asFloat }.getOrNull()
    }

    override fun write(values: Map<String, Any>) {
        // No file means the mod has never run. Writing one now would be a guess
        // at a schema we have only seen part of, so leave it for the mod.
        val root = root() ?: return

        for ((key, value) in values) {
            val (holder, leaf) = resolve(root, key, create = true) ?: continue
            when (value) {
                is Boolean -> holder.add(leaf, JsonPrimitive(value))
                is Int -> holder.add(leaf, JsonPrimitive(value))
                is Float -> holder.add(leaf, JsonPrimitive(value))
                is String -> holder.add(leaf, JsonPrimitive(value))
                else -> continue
            }
        }

        runCatching {
            Files.writeString(path, gson.toJson(root))
        }.onFailure {
            logger.warn("Could not write ${path.fileName}", it)
        }
    }
}

/** A `.properties` config, as Lithium and Simple Voice Chat use. */
class PropertiesConfigStore(path: Path) : ModConfigStore(path) {

    private fun load(): Properties? {
        if (!exists()) {
            return null
        }

        return runCatching {
            Properties().apply {
                Files.newBufferedReader(path).use { load(it) }
            }
        }.onFailure {
            logger.warn("Could not read ${path.fileName}", it)
        }.getOrNull()
    }

    override fun readBoolean(key: String): Boolean? =
        load()?.getProperty(key)?.toBooleanStrictOrNull()

    override fun readInt(key: String): Int? =
        load()?.getProperty(key)?.toIntOrNull()

    override fun readFloat(key: String): Float? =
        load()?.getProperty(key)?.toFloatOrNull()

    override fun write(values: Map<String, Any>) {
        val properties = load() ?: return

        for ((key, value) in values) {
            properties.setProperty(key, value.toString())
        }

        runCatching {
            Files.newBufferedWriter(path).use {
                properties.store(it, "Written by Tsunami. Edit in the ClickGUI.")
            }
        }.onFailure {
            logger.warn("Could not write ${path.fileName}", it)
        }
    }
}
