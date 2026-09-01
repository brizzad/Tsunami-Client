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

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Fixture trimmed from the real `shieldstatus.json` WalksyLib wrote, keeping
 * the shape that makes this store necessary: named categories holding named
 * groups holding named options, with the value on a `value` field beside the
 * type and the min/max the mod also stores.
 */
class NamedRecordConfigStoreTest {

    @TempDir
    lateinit var dir: Path

    private val fixture = """
        [
          {
            "name": "General",
            "options": [],
            "groups": [
              {
                "name": "Global Options",
                "expanded": true,
                "options": [
                  { "name": "Mod Enabled", "type": "boolean", "value": true, "min": null }
                ]
              }
            ]
          },
          {
            "name": "Color",
            "options": [],
            "groups": [
              {
                "name": "General Options",
                "expanded": true,
                "options": [
                  { "name": "Self State Only", "type": "boolean", "value": false, "min": null }
                ]
              },
              {
                "name": "Enabled Shield Options",
                "expanded": true,
                "options": [
                  {
                    "name": "Enabled Color",
                    "type": "walksylibcolor",
                    "value": { "r": 0, "g": 255, "b": 0, "a": 255, "value": -16711936, "rainbow": false },
                    "min": null
                  }
                ]
              }
            ]
          }
        ]
    """.trimIndent()

    private fun store(): NamedRecordConfigStore {
        val path = dir.resolve("shieldstatus.json")
        Files.writeString(path, fixture)
        return NamedRecordConfigStore(path)
    }

    private fun reread() = JsonParser.parseString(Files.readString(dir.resolve("shieldstatus.json"))).asJsonArray

    @Test
    fun `reads a scalar through a category group option path`() {
        val store = store()

        assertEquals(true, store.readBoolean("General/Global Options/Mod Enabled"))
        assertEquals(false, store.readBoolean("Color/General Options/Self State Only"))
    }

    @Test
    fun `reads an object value whole`() {
        val colour = store().readObject("Color/Enabled Shield Options/Enabled Color")

        assertNotNull(colour)
        assertEquals(255, colour!!.get("g").asInt)
    }

    @Test
    fun `a path that names nothing reads null`() {
        val store = store()

        assertNull(store.readBoolean("General/Global Options/No Such Option"))
        assertNull(store.readBoolean("General/No Such Group/Mod Enabled"))
        assertNull(store.readBoolean("No Such Category/Global Options/Mod Enabled"))
        // A path of the wrong shape is refused rather than half-matched.
        assertNull(store.readBoolean("General/Global Options"))
    }

    @Test
    fun `a write replaces the value and leaves the rest of the record alone`() {
        val store = store()
        store.write(mapOf("General/Global Options/Mod Enabled" to false))

        assertEquals(false, store.readBoolean("General/Global Options/Mod Enabled"))

        val option = reread()[0].asJsonObject
            .getAsJsonArray("groups")[0].asJsonObject
            .getAsJsonArray("options")[0].asJsonObject
        assertEquals("boolean", option.get("type").asString, "the mod's own type field was lost")
        assertEquals(true, option.has("min"), "the mod's own min field was lost")
    }

    @Test
    fun `an object value can be written whole`() {
        val store = store()
        val red = JsonObject().apply {
            addProperty("r", 255)
            addProperty("g", 0)
            addProperty("b", 0)
            addProperty("a", 255)
            addProperty("value", -65536)
            addProperty("rainbow", false)
        }
        store.write(mapOf("Color/Enabled Shield Options/Enabled Color" to red))

        val stored = store.readObject("Color/Enabled Shield Options/Enabled Color")
        assertEquals(255, stored!!.get("r").asInt)
        assertEquals(-65536, stored.get("value").asInt)
    }

    /**
     * The mod owns the shape of this file. Inventing a record for a setting it
     * does not have would hand it a config it cannot load, so an unknown path
     * is dropped with a log line instead.
     */
    @Test
    fun `an unknown path is not added to the file`() {
        val store = store()
        store.write(mapOf("Color/General Options/Invented Option" to true))

        assertNull(store.readBoolean("Color/General Options/Invented Option"))
        val options = reread()[1].asJsonObject
            .getAsJsonArray("groups")[0].asJsonObject
            .getAsJsonArray("options")
        assertEquals(1, options.size())
    }

    @Test
    fun `an absent file reads null and a write does not create one`() {
        val path = dir.resolve("never-written.json")
        val store = NamedRecordConfigStore(path)

        assertNull(store.readBoolean("General/Global Options/Mod Enabled"))
        store.write(mapOf("General/Global Options/Mod Enabled" to false))
        assertEquals(true, Files.notExists(path))
    }
}
