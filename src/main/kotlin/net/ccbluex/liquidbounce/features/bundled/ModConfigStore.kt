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
    abstract fun readString(key: String): String?
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

        /** TOML, as MoreCulling and Ixeris write it. Sections become dotted paths. */
        fun toml(fileName: String) =
            LineConfigStore(configDir.resolve(fileName), separator = " = ", sections = true)

        /** BadOptimizations' `key: value` text file. Flat, no sections. */
        fun colonSeparated(fileName: String) =
            LineConfigStore(configDir.resolve(fileName), separator = ": ", sections = false)
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

    /**
     * Splits a dotted path, treating a backslash-escaped dot as part of a key name.
     *
     * A dot normally means "descend into this object", which is how Sodium nests by
     * category. Jade needs both at once: its plugin sections are real objects, but the
     * keys inside them are flat strings that themselves contain dots -
     * `harvest_tool.effective_tool` is one key, not two levels. Splitting that naively
     * writes a key Jade never reads and leaves the real one untouched, which looks
     * exactly like a setting that silently does nothing.
     */
    /** A dot that is not escaped with a backslash. */
    private val unescapedDot = Regex("""(?<!\\)\.""")

    private fun segments(key: String): List<String> =
        key.split(unescapedDot).map { it.replace("\\.", ".") }

    private fun resolve(root: JsonObject, key: String, create: Boolean): Pair<JsonObject, String>? {
        val parts = segments(key)
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

    override fun readString(key: String): String? {
        val root = root() ?: return null
        val (holder, leaf) = resolve(root, key, create = false) ?: return null
        val element = holder.get(leaf) ?: return null
        return runCatching { element.asString }.getOrNull()
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

    override fun readString(key: String): String? =
        load()?.getProperty(key)

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

/**
 * A line-oriented `key <sep> value` config, edited in place.
 *
 * Covers the two shapes that are not JSON here: TOML, as MoreCulling and
 * Ixeris write it, and the `key: value` text file BadOptimizations writes.
 * Both are flat lists of assignments with `#` comments, and TOML adds
 * `[section]` headers, which this addresses as a dotted path -
 * `modCompatibility.minecraft`.
 *
 * ## Why it rewrites lines rather than reserialising
 *
 * Every one of these files is mostly comments, and the comments are the
 * documentation: BadOptimizations explains what each optimisation costs, and
 * Ixeris warns which of its options are debug-only. A parse-and-reserialise
 * would drop all of it and hand the player back a file they can no longer
 * read. [PropertiesConfigStore] does exactly that, which is a good reason not
 * to reuse it here.
 *
 * So a write finds the line that assigns the key and replaces only the value
 * on it. Everything else in the file - comments, blank lines, ordering,
 * sections, and any key this bridge does not know about - is passed through
 * untouched.
 *
 * This is not a TOML parser and does not try to be. Multi-line values, inline
 * tables and arrays-of-tables are not supported; arrays are read as their raw
 * text and never written. Nothing bridged here uses them, and a mod that did
 * would want its own store rather than a guess.
 */
class LineConfigStore(
    path: Path,
    private val separator: String,
    private val sections: Boolean,
) : ModConfigStore(path) {

    private fun lines(): List<String>? {
        if (!exists()) {
            return null
        }

        return runCatching {
            Files.readAllLines(path)
        }.onFailure {
            logger.warn("Could not read ${path.fileName}", it)
        }.getOrNull()
    }

    /** The assignment on a line, as section-qualified key to raw value. */
    private fun entries(lines: List<String>): Map<String, String> {
        val found = LinkedHashMap<String, String>()
        var section = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }

            if (sections && trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.removeSurrounding("[", "]").trim() + "."
                continue
            }

            val at = trimmed.indexOf(separator.trim())
            if (at <= 0) {
                continue
            }

            val key = trimmed.substring(0, at).trim()
            val value = trimmed.substring(at + separator.trim().length).trim()
            found[section + key] = value
        }

        return found
    }

    /** TOML quotes its strings; the readers below want the text inside. */
    private fun unquote(raw: String) = raw.trim().removeSurrounding("\"")

    private fun raw(key: String): String? = lines()?.let { entries(it)[key] }

    override fun readBoolean(key: String) = raw(key)?.lowercase()?.toBooleanStrictOrNull()

    override fun readInt(key: String) = raw(key)?.toIntOrNull()

    override fun readFloat(key: String) = raw(key)?.toFloatOrNull()

    override fun readString(key: String) = raw(key)?.let(::unquote)

    /**
     * Quotes a string and leaves everything else bare, which is what both
     * formats want: `true`, `11`, `2.0`, `"DEFAULT"`.
     */
    private fun format(value: Any) = when (value) {
        is Boolean, is Int, is Long, is Float, is Double -> value.toString()
        else -> "\"$value\""
    }

    override fun write(values: Map<String, Any>) {
        val existing = lines() ?: return
        val remaining = values.toMutableMap()
        val out = ArrayList<String>(existing.size)
        var section = ""

        for (line in existing) {
            val trimmed = line.trim()

            if (sections && trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.removeSurrounding("[", "]").trim() + "."
                out.add(line)
                continue
            }

            val at = trimmed.indexOf(separator.trim())
            if (trimmed.isEmpty() || trimmed.startsWith("#") || at <= 0) {
                out.add(line)
                continue
            }

            val key = section + trimmed.substring(0, at).trim()
            val replacement = remaining.remove(key)
            if (replacement == null) {
                out.add(line)
                continue
            }

            // Keep the line's own indentation and spelling of the separator.
            val indent = line.takeWhile { it.isWhitespace() }
            val name = trimmed.substring(0, at).trim()
            out.add("$indent$name$separator${format(replacement)}")
        }

        /*
         * A key the file has never held - a mod that only writes a setting
         * once it differs from its default. Adding it is the only way to set
         * it, and *where* it goes decides whether it works.
         *
         * A bare key appended to the end of a sectioned file belongs to
         * whichever section happens to be last, so a top-level key has to go
         * in before the first section header. A key that names a section this
         * file does not have is skipped rather than guessed at: writing it
         * into the wrong table is worse than not writing it.
         */
        val firstSection = out.indexOfFirst {
            val t = it.trim()
            sections && t.startsWith("[") && t.endsWith("]")
        }

        for ((key, value) in remaining) {
            if (sections && key.contains('.')) {
                logger.warn("Not adding ${path.fileName} key $key: its section is not in the file")
                continue
            }

            val line = "$key$separator${format(value)}"
            if (firstSection >= 0) {
                out.add(firstSection, line)
            } else {
                out.add(line)
            }
        }

        runCatching {
            Files.write(path, out)
        }.onFailure {
            logger.warn("Could not write ${path.fileName}", it)
        }
    }
}
