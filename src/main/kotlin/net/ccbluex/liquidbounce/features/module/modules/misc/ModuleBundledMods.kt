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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.bundled.ModConfigStore
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * BundledMods
 *
 * Settings for the mods Tsunami ships alongside itself, in the ClickGUI rather
 * than in six separate screens.
 *
 * ## Why this exists
 *
 * Tsunami bundles established mods instead of writing worse copies of them, and
 * that is the right call - but it means a player who wants to turn motion blur
 * down has to leave the client UI, find that mod, and use its screen. A client
 * whose settings live in seven places is not a client, it is a modpack.
 *
 * ## The honest limitation
 *
 * Every mod here reads its config once at startup and keeps it in memory. This
 * writes to the same file the mod reads, which means **changes take effect the
 * next time the game starts**, not immediately. That is stated on every group
 * rather than hidden, because a switch that looks live and is not is worse than
 * one that tells you to restart.
 *
 * ## Coverage
 *
 * A group appears only when the mod is actually installed, so in a development
 * run most of these are absent and the module is nearly empty. Every key below
 * was read from the mod itself - from its config file where one existed, or
 * from the field names in its jar - rather than guessed at.
 *
 * Not yet covered, and why: Jade, Shield Statuses, Replay Mod and WorldEdit CUI
 * were not inspected, and AppleSkin stores its settings through NeoForge's
 * TOML config spec, which this bridge does not speak. Simple Voice Chat is a
 * deliberate hold rather than an oversight - it configures a microphone and a
 * push-to-talk key, and half-bridging that is how someone ends up transmitting
 * when they think they are muted.
 */
object ModuleBundledMods : ClientModule("BundledMods", ModuleCategories.MISC) {

    init {
        tree(Sodium)
        tree(ImmediatelyFast)
        tree(EntityCulling)
        tree(SkinLayers)
    }

    /**
     * Sodium. Keys verified against a real `sodium-options.json`, which nests
     * options under a category, hence the dotted paths.
     */
    object Sodium : ValueGroup("Sodium") {
        private val store = ModConfigStore.json("sodium-options.json")

        val entityCulling by boolean("EntityCulling", store.readBoolean(KEY_ENTITY_CULLING) ?: true)
            .onChanged { write(KEY_ENTITY_CULLING to it) }

        val fogOcclusion by boolean("FogOcclusion", store.readBoolean(KEY_FOG_OCCLUSION) ?: true)
            .onChanged { write(KEY_FOG_OCCLUSION to it) }

        val blockFaceCulling by boolean("BlockFaceCulling", store.readBoolean(KEY_FACE_CULLING) ?: true)
            .onChanged { write(KEY_FACE_CULLING to it) }

        val animateOnlyVisible by boolean("AnimateOnlyVisibleTextures", store.readBoolean(KEY_ANIMATE) ?: true)
            .onChanged { write(KEY_ANIMATE to it) }

        /** Zero lets Sodium pick, which is almost always the right answer. */
        val chunkBuilderThreads by int("ChunkBuilderThreads", store.readInt(KEY_THREADS) ?: 0, 0..16)
            .onChanged { write(KEY_THREADS to it) }

        private fun write(pair: Pair<String, Any>) = applyTo(store, "sodium", pair)
    }

    /** ImmediatelyFast. Keys verified against a real `immediatelyfast.json`. */
    object ImmediatelyFast : ValueGroup("ImmediatelyFast") {
        private val store = ModConfigStore.json("immediatelyfast.json")

        val enhancedBatching by boolean("EnhancedBatching", store.readBoolean("enhanced_batching") ?: true)
            .onChanged { write("enhanced_batching" to it) }

        val fastTextLookup by boolean("FastTextLookup", store.readBoolean("fast_text_lookup") ?: true)
            .onChanged { write("fast_text_lookup" to it) }

        val fontAtlasResizing by boolean("FontAtlasResizing", store.readBoolean("font_atlas_resizing") ?: true)
            .onChanged { write("font_atlas_resizing" to it) }

        val mapAtlasGeneration by boolean("MapAtlasGeneration", store.readBoolean("map_atlas_generation") ?: true)
            .onChanged { write("map_atlas_generation" to it) }

        private fun write(pair: Pair<String, Any>) = applyTo(store, "immediatelyfast", pair)
    }

    /** EntityCulling. Keys read from `Config` in the mod jar. */
    object EntityCulling : ValueGroup("EntityCulling") {
        private val store = ModConfigStore.json("entityculling.json")

        val tickCulling by boolean("TickCulling", store.readBoolean("tickCulling") ?: true)
            .onChanged { write("tickCulling" to it) }

        val nametagsThroughWalls by boolean(
            "NametagsThroughWalls",
            store.readBoolean("renderNametagsThroughWalls") ?: false
        ).onChanged { write("renderNametagsThroughWalls" to it) }

        val solidLeaves by boolean("SolidLeaves", store.readBoolean("solidLeaves") ?: true)
            .onChanged { write("solidLeaves" to it) }

        /** How far culling traces. Higher costs more to compute, not less. */
        val tracingDistance by int("TracingDistance", store.readInt("tracingDistance") ?: 128, 32..512)
            .onChanged { write("tracingDistance" to it) }

        private fun write(pair: Pair<String, Any>) = applyTo(store, "entityculling", pair)
    }

    /** Natural Motion Blur. Keys read from `ConfigEntries` in the mod jar. */
    // MotionBlur is intentionally not bridged here. It is no longer a bundled jar:
    // the engine was merged into the client and is configured by the MotionBlur
    // module directly, so a bridge would be a second UI writing a config file that
    // nothing reads any more.

    /** 3D Skin Layers. Keys read from `Config` in the mod jar. */
    object SkinLayers : ValueGroup("SkinLayers") {
        private val store = ModConfigStore.json("skinlayers.json")

        val hat by boolean("Hat", store.readBoolean("enableHat") ?: true)
            .onChanged { write("enableHat" to it) }

        val jacket by boolean("Jacket", store.readBoolean("enableJacket") ?: true)
            .onChanged { write("enableJacket" to it) }

        val sleeves by boolean("Sleeves", store.readBoolean("enableLeftSleeve") ?: true)
            .onChanged { write("enableLeftSleeve" to it, "enableRightSleeve" to it) }

        val pants by boolean("Pants", store.readBoolean("enableLeftPants") ?: true)
            .onChanged { write("enableLeftPants" to it, "enableRightPants" to it) }

        /** Player heads on blocks and in inventories get the same treatment. */
        val skulls by boolean("Skulls", store.readBoolean("enableSkulls") ?: true)
            .onChanged { write("enableSkulls" to it) }

        /** Beyond this, layers fall back to the flat vanilla rendering. */
        val renderDistance by int("RenderDistance", store.readInt("renderDistanceLOD") ?: 24, 4..64)
            .onChanged { write("renderDistanceLOD" to it) }

        private fun write(vararg pairs: Pair<String, Any>) =
            applyTo(store, "3dskinlayers", *pairs)
    }

    private const val KEY_ENTITY_CULLING = "performance.use_entity_culling"
    private const val KEY_FOG_OCCLUSION = "performance.use_fog_occlusion"
    private const val KEY_FACE_CULLING = "performance.use_block_face_culling"
    private const val KEY_ANIMATE = "performance.animate_only_visible_textures"
    private const val KEY_THREADS = "performance.chunk_builder_threads"

    /**
     * Writes a change through to the mod, or explains why it did nothing.
     *
     * Silence would be the wrong behaviour on both failure paths: a mod that is
     * not installed and a config file that has never been written look
     * identical from the ClickGUI, and in both cases the switch the player just
     * moved is not going to do anything.
     */
    private fun applyTo(store: ModConfigStore, modId: String, vararg pairs: Pair<String, Any>) {
        if (!ModConfigStore.isModLoaded(modId)) {
            chat("§7$modId is not installed, so that setting has nothing to change.")
            return
        }

        store.write(pairs.toMap())
        logger.info("Wrote ${pairs.size} value(s) to the $modId config")
        chat("§7Saved to $modId. It applies the next time the game starts.")
    }
}
