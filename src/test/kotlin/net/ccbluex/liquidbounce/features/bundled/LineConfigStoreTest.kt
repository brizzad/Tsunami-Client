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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * [LineConfigStore] writes into files other mods own, so the thing worth
 * testing is not that a value round-trips - it is that everything else in the
 * file survives.
 *
 * These fixtures are trimmed from the real configs MoreCulling, Ixeris and
 * BadOptimizations wrote in `gameDir/local/config`, comments and all.
 */
class LineConfigStoreTest {

    @TempDir
    lateinit var dir: Path

    private fun toml(text: String): LineConfigStore {
        val path = dir.resolve("config.toml")
        Files.writeString(path, text)
        return LineConfigStore(path, separator = " = ", sections = true)
    }

    private fun colon(text: String): LineConfigStore {
        val path = dir.resolve("config.txt")
        Files.writeString(path, text)
        return LineConfigStore(path, separator = ": ", sections = false)
    }

    private fun read(name: String) = Files.readString(dir.resolve(name))

    private val moreCulling = """
        version = 1
        enableSodiumMenu = true
        dontCull = []
        signTextCulling = true
        useItemFrameLOD = true
        itemFrameLODRange = 11
        itemFrame3FaceCullingRange = 2.0
        leavesCullingMode = "DEFAULT"
        includeMangroveRoots = false

        [modCompatibility]
        minecraft = true
    """.trimIndent()

    private val badOptimizations = """
        # BadOptimizations configuration
        # Toggle and configure optimizations here.

        # Whether we should cancel updating the lightmap if not needed.
        enable_lightmap_caching: true
        # Higher values mean less frequent updates.
        lightmap_update_frequency: 2
    """.trimIndent()

    @Test
    fun `reads every scalar type it is asked for`() {
        val store = toml(moreCulling)

        assertEquals(true, store.readBoolean("signTextCulling"))
        assertEquals(false, store.readBoolean("includeMangroveRoots"))
        assertEquals(11, store.readInt("itemFrameLODRange"))
        assertEquals(2.0f, store.readFloat("itemFrame3FaceCullingRange"))
        assertEquals("DEFAULT", store.readString("leavesCullingMode"))
    }

    @Test
    fun `addresses a section as a dotted path`() {
        val store = toml(moreCulling)

        assertEquals(true, store.readBoolean("modCompatibility.minecraft"))
        // The same name outside its section must not be found by accident.
        assertNull(store.readBoolean("minecraft"))
    }

    @Test
    fun `reads a colon separated file`() {
        val store = colon(badOptimizations)

        assertEquals(true, store.readBoolean("enable_lightmap_caching"))
        assertEquals(2, store.readInt("lightmap_update_frequency"))
    }

    @Test
    fun `a missing key reads as null rather than a default`() {
        val store = toml(moreCulling)

        assertNull(store.readBoolean("noSuchKey"))
        assertNull(store.readInt("alsoMissing"))
    }

    @Test
    fun `a write changes the value and nothing else`() {
        val store = toml(moreCulling)
        store.write(mapOf("signTextCulling" to false, "itemFrameLODRange" to 4))

        assertEquals(false, store.readBoolean("signTextCulling"))
        assertEquals(4, store.readInt("itemFrameLODRange"))

        val text = read("config.toml")
        // Untouched keys, the array this store does not understand, and the
        // section header all survive.
        assertTrue(text.contains("dontCull = []"), "array line was rewritten: $text")
        assertTrue(text.contains("version = 1"))
        assertTrue(text.contains("[modCompatibility]"))
        assertTrue(text.contains("leavesCullingMode = \"DEFAULT\""))
    }

    @Test
    fun `comments survive a write`() {
        val store = colon(badOptimizations)
        store.write(mapOf("enable_lightmap_caching" to false))

        val text = read("config.txt")
        assertEquals(false, store.readBoolean("enable_lightmap_caching"))
        assertTrue(text.contains("# BadOptimizations configuration"), "header comment lost: $text")
        assertTrue(
            text.contains("# Whether we should cancel updating the lightmap if not needed."),
            "per-key comment lost: $text",
        )
        assertTrue(text.contains("# Higher values mean less frequent updates."))
    }

    @Test
    fun `a string is written quoted and a number is not`() {
        val store = toml(moreCulling)
        store.write(mapOf("leavesCullingMode" to "FAST", "itemFrameLODRange" to 7))

        val text = read("config.toml")
        assertTrue(text.contains("leavesCullingMode = \"FAST\""), text)
        assertTrue(text.contains("itemFrameLODRange = 7"), text)
        assertEquals("FAST", store.readString("leavesCullingMode"))
    }

    @Test
    fun `a key the file never held is appended`() {
        val store = toml(moreCulling)
        store.write(mapOf("rainCulling" to false))

        assertEquals(false, store.readBoolean("rainCulling"))
    }

    /**
     * A sectioned key that is not already in the file cannot be appended
     * safely - a bare `a.b = x` line at the end of a TOML file lands in
     * whichever section happens to be last. Better to skip it and say so than
     * to write it into the wrong table.
     */
    @Test
    fun `a sectioned key the file never held is refused rather than misplaced`() {
        val store = toml(moreCulling)
        store.write(mapOf("someOtherMod.enabled" to true))

        assertNull(store.readBoolean("someOtherMod.enabled"))
        assertTrue(!read("config.toml").contains("someOtherMod.enabled"))
    }

    @Test
    fun `an absent file reads null and a write does not create one`() {
        val path = dir.resolve("never-written.toml")
        val store = LineConfigStore(path, separator = " = ", sections = true)

        assertNull(store.readBoolean("anything"))
        store.write(mapOf("anything" to true))
        assertTrue(Files.notExists(path), "wrote a config the mod has never written")
    }
}
