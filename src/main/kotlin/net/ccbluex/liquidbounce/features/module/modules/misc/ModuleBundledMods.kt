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
import net.ccbluex.liquidbounce.config.types.list.Tagged
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
 * Not yet covered, and why: Replay Mod and WorldEdit CUI are off by default and
 * have never written a config here, so there are no keys to read. Simple Voice Chat is a
 * deliberate hold rather than an oversight - it configures a microphone and a
 * push-to-talk key, and half-bridging that is how someone ends up transmitting
 * when they think they are muted.
 */
object ModuleBundledMods : ClientModule("BundledMods", ModuleCategories.MISC) {

    init {
        tree(Sodium)
        tree(SodiumExtra)
        tree(ImmediatelyFast)
        tree(EntityCulling)
        tree(SkinLayers)
        tree(Jade)
        tree(AppleSkin)
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


    /**
     * Jade, the WAILA-style block and entity readout.
     *
     * Every key below was read from a real `config/jade/jade.json` written by Jade
     * `26.2.11+fabric`, and every enum's constants from the mod's own classes
     * (`IWailaConfig`, `ModNameProvider`, `RegistryNameProvider`, `PetArmorProvider`)
     * rather than guessed at.
     *
     * ## Two things make Jade awkward where the others were not
     *
     * Its config lives in a subdirectory, `jade/jade.json`, rather than beside the rest.
     *
     * More importantly, **its plugin keys contain literal dots**. Inside the
     * `plugin.minecraft` object, `harvest_tool.effective_tool` is one flat key, not two
     * levels of nesting. A plain dotted path would create bogus nesting and leave the
     * real key untouched - a setting that looks saved and does nothing. Those keys are
     * escaped as `\.` here, and [JsonConfigStore] splits only on unescaped dots.
     *
     * ## Deliberately not bridged
     *
     * `overlay.activeTheme` is a registry id whose valid values depend on which themes
     * are installed, so a fixed list here would go stale. `enableProfiles` and
     * `profileIndex` swap which config file Jade reads, and this bridge writes to one
     * directly - exposing them would make every other setting here edit a file Jade is
     * no longer reading. `history.*` is Jade's own bookkeeping, and `general.debug` is
     * developer output.
     */
    object Jade : ValueGroup("Jade") {

        init {
            treeAll(General, Overlay, Content, Blocks, Entities, Accessibility)
        }

        /** What Jade looks at, and when it shows itself. */
        object General : ValueGroup("General") {
            val tooltip by boolean("Tooltip", jadeBool("general.displayTooltip", true))
                .onChanged { write("general.displayTooltip" to it) }

            val blocks by boolean("Blocks", jadeBool("general.displayBlocks", true))
                .onChanged { write("general.displayBlocks" to it) }

            val entities by boolean("Entities", jadeBool("general.displayEntities", true))
                .onChanged { write("general.displayEntities" to it) }

            val bosses by boolean("Bosses", jadeBool("general.displayBosses", true))
                .onChanged { write("general.displayBosses" to it) }

            /** Toggle with the key, hold the key, or a cut-down always-on readout. */
            val displayMode by enumChoice(
                "DisplayMode",
                jadeEnum(JadeDisplayMode.entries, "general.displayMode", JadeDisplayMode.TOGGLE)
            ).onChanged { write("general.displayMode" to it.key) }

            /** Whether looking at a fluid counts as looking at a block. */
            val fluidMode by enumChoice(
                "FluidMode",
                jadeEnum(JadeFluidMode.entries, "general.fluidMode", JadeFluidMode.ANY)
            ).onChanged { write("general.fluidMode" to it.key) }

            /** Trace from the camera or from the eyes - they differ in third person. */
            val perspective by enumChoice(
                "Perspective",
                jadeEnum(JadePerspective.entries, "general.perspectiveMode", JadePerspective.CAMERA)
            ).onChanged { write("general.perspectiveMode" to it.key) }

            /** Extra blocks of reach for the readout only. Interaction is unchanged. */
            val extendedReach by int("ExtendedReach", jadeInt("general.extendedReach", 0), 0..32)
                .onChanged { write("general.extendedReach" to it) }

            val modNameOnItems by boolean(
                "ModNameOnItemTooltips", jadeBool("general.itemModNameTooltip", true)
            ).onChanged { write("general.itemModNameTooltip" to it) }

            /** What gives way when the readout and a boss bar want the same space. */
            val bossBarOverlap by enumChoice(
                "BossBarOverlap",
                jadeEnum(JadeBossBar.entries, "general.bossBarOverlapMode", JadeBossBar.PUSH_DOWN)
            ).onChanged { write("general.bossBarOverlapMode" to it.key) }

            /** Report a disguised block as what it is pretending to be. */
            val builtinCamouflage by boolean(
                "BuiltinCamouflage", jadeBool("general.builtinCamouflage", true)
            ).onChanged { write("general.builtinCamouflage" to it) }

            val hideWhileTabList by boolean(
                "HideWhileTabList", jadeBool("general.hideFromTabList", true)
            ).onChanged { write("general.hideFromTabList" to it) }

            val hideInScreens by boolean("HideInScreens", jadeBool("general.hideFromGUIs", true))
                .onChanged { write("general.hideFromGUIs" to it) }

            private fun write(vararg pairs: Pair<String, Any>) = applyTo(jadeStore, JADE_ID, *pairs)
        }

        /** Where the readout sits, and how it is drawn. */
        object Overlay : ValueGroup("Overlay") {
            /** Fraction across the screen, 0 left to 1 right. */
            val posX by float("PositionX", jadeFloat("overlay.overlayPosX", 0.5f), 0f..1f)
                .onChanged { write("overlay.overlayPosX" to it) }

            /** Fraction down the screen, 0 top to 1 bottom. */
            val posY by float("PositionY", jadeFloat("overlay.overlayPosY", 1f), 0f..1f)
                .onChanged { write("overlay.overlayPosY" to it) }

            /** Which point of the box the position refers to. */
            val anchorX by float("AnchorX", jadeFloat("overlay.overlayAnchorX", 0.5f), 0f..1f)
                .onChanged { write("overlay.overlayAnchorX" to it) }

            val anchorY by float("AnchorY", jadeFloat("overlay.overlayAnchorY", 0f), 0f..1f)
                .onChanged { write("overlay.overlayAnchorY" to it) }

            val scale by float("Scale", jadeFloat("overlay.overlayScale", 1f), 0.25f..2f)
                .onChanged { write("overlay.overlayScale" to it) }

            /** Shrink rather than overflow once the box passes this share of the screen. */
            val autoScaleThreshold by float(
                "AutoScaleThreshold", jadeFloat("overlay.autoScaleThreshold", 0.4f), 0f..1f
            ).onChanged { write("overlay.autoScaleThreshold" to it) }

            val backgroundAlpha by float("BackgroundAlpha", jadeFloat("overlay.alpha", 0.7f), 0f..1f)
                .onChanged { write("overlay.alpha" to it) }

            val icon by enumChoice(
                "Icon", jadeEnum(JadeIconMode.entries, "overlay.iconMode", JadeIconMode.TOP)
            ).onChanged { write("overlay.iconMode" to it.key) }

            val animation by boolean("Animation", jadeBool("overlay.animation", true))
                .onChanged { write("overlay.animation" to it) }

            /** Seconds the readout lingers after you look away. 0 hides it at once. */
            val disappearingDelay by float(
                "DisappearingDelay", jadeFloat("overlay.disappearingDelay", 0f), 0f..5f
            ).onChanged { write("overlay.disappearingDelay" to it) }

            private fun write(vararg pairs: Pair<String, Any>) = applyTo(jadeStore, JADE_ID, *pairs)
        }

        /** Jade's own lines: what is named, and what identifying detail comes with it. */
        object Content : ValueGroup("Content") {
            val objectName by boolean("ObjectName", jadeBool("plugin.jade.object_name", true))
                .onChanged { write("plugin.jade.object_name" to it) }

            val modName by enumChoice(
                "ModName", jadeEnum(JadeModName.entries, "plugin.jade.mod_name", JadeModName.ON)
            ).onChanged { write("plugin.jade.mod_name" to it.key) }

            val modNameTranslated by boolean(
                "ModNameTranslated", jadeBool("plugin.jade.mod_name\\.translated", true)
            ).onChanged { write("plugin.jade.mod_name\\.translated" to it) }

            /** The registry id, e.g. `minecraft:oak_log`. */
            val registryName by enumChoice(
                "RegistryName",
                jadeEnum(JadeRegistryName.entries, "plugin.jade.registry_name", JadeRegistryName.OFF)
            ).onChanged { write("plugin.jade.registry_name" to it.key) }

            val registryNameSpecialIds by boolean(
                "RegistryNameSpecialIds", jadeBool("plugin.jade.registry_name\\.special", false)
            ).onChanged { write("plugin.jade.registry_name\\.special" to it) }

            val coordinates by boolean("Coordinates", jadeBool("plugin.jade.coordinates", false))
                .onChanged { write("plugin.jade.coordinates" to it) }

            /** Coordinates relative to you rather than absolute. */
            val coordinatesRelative by boolean(
                "CoordinatesRelative", jadeBool("plugin.jade.coordinates\\.rel", false)
            ).onChanged { write("plugin.jade.coordinates\\.rel" to it) }

            val distance by boolean("Distance", jadeBool("plugin.jade.distance", false))
                .onChanged { write("plugin.jade.distance" to it) }

            val blockStates by boolean("BlockStates", jadeBool("plugin.jade.block_states", false))
                .onChanged { write("plugin.jade.block_states" to it) }

            val blockProperties by boolean(
                "BlockProperties", jadeBool("plugin.jade.block_properties", false)
            ).onChanged { write("plugin.jade.block_properties" to it) }

            val blockFace by boolean("BlockFace", jadeBool("plugin.jade.block_face", false))
                .onChanged { write("plugin.jade.block_face" to it) }

            val lootTable by boolean("LootTable", jadeBool("plugin.jade.loot_table", false))
                .onChanged { write("plugin.jade.loot_table" to it) }

            private fun write(vararg pairs: Pair<String, Any>) = applyTo(jadeStore, JADE_ID, *pairs)
        }

        /** What Jade reports about blocks. */
        object Blocks : ValueGroup("Blocks") {
            val harvestTool by boolean("HarvestTool", jadeBool("plugin.minecraft.harvest_tool", true))
                .onChanged { write("plugin.minecraft.harvest_tool" to it) }

            val showEffectiveTool by boolean(
                "ShowEffectiveTool", jadeBool("plugin.minecraft.harvest_tool\\.effective_tool", true)
            ).onChanged { write("plugin.minecraft.harvest_tool\\.effective_tool" to it) }

            val showUnbreakable by boolean(
                "ShowUnbreakable", jadeBool("plugin.minecraft.harvest_tool\\.show_unbreakable", false)
            ).onChanged { write("plugin.minecraft.harvest_tool\\.show_unbreakable" to it) }

            val harvestInCreative by boolean(
                "HarvestInCreative", jadeBool("plugin.minecraft.harvest_tool\\.creative", false)
            ).onChanged { write("plugin.minecraft.harvest_tool\\.creative" to it) }

            val harvestOnNewLine by boolean(
                "HarvestOnNewLine", jadeBool("plugin.minecraft.harvest_tool\\.new_line", false)
            ).onChanged { write("plugin.minecraft.harvest_tool\\.new_line" to it) }

            val breakingProgress by boolean(
                "BreakingProgress", jadeBool("plugin.minecraft.breaking_progress", true)
            ).onChanged { write("plugin.minecraft.breaking_progress" to it) }

            val cropProgress by boolean("CropProgress", jadeBool("plugin.minecraft.crop_progress", true))
                .onChanged { write("plugin.minecraft.crop_progress" to it) }

            val itemStorage by boolean("ItemStorage", jadeBool("plugin.minecraft.item_storage", true))
                .onChanged { write("plugin.minecraft.item_storage" to it) }

            val itemsPerLine by int(
                "ItemsPerLine", jadeInt("plugin.minecraft.item_storage\\.items_per_line", 9), 1..18
            ).onChanged { write("plugin.minecraft.item_storage\\.items_per_line" to it) }

            val fluidStorage by boolean("FluidStorage", jadeBool("plugin.minecraft.fluid_storage", true))
                .onChanged { write("plugin.minecraft.fluid_storage" to it) }

            val energyStorage by boolean("EnergyStorage", jadeBool("plugin.minecraft.energy_storage", true))
                .onChanged { write("plugin.minecraft.energy_storage" to it) }

            val redstone by boolean("Redstone", jadeBool("plugin.minecraft.redstone", true))
                .onChanged { write("plugin.minecraft.redstone" to it) }

            val mobSpawner by boolean("MobSpawner", jadeBool("plugin.minecraft.mob_spawner", true))
                .onChanged { write("plugin.minecraft.mob_spawner" to it) }

            val brewingStand by boolean("BrewingStand", jadeBool("plugin.minecraft.brewing_stand", true))
                .onChanged { write("plugin.minecraft.brewing_stand" to it) }

            val furnace by boolean("Furnace", jadeBool("plugin.minecraft.furnace", true))
                .onChanged { write("plugin.minecraft.furnace" to it) }

            val jukebox by boolean("Jukebox", jadeBool("plugin.minecraft.jukebox", true))
                .onChanged { write("plugin.minecraft.jukebox" to it) }

            val noteBlock by boolean("NoteBlock", jadeBool("plugin.minecraft.note_block", true))
                .onChanged { write("plugin.minecraft.note_block" to it) }

            val beehive by boolean("Beehive", jadeBool("plugin.minecraft.beehive", true))
                .onChanged { write("plugin.minecraft.beehive" to it) }

            val lectern by boolean("Lectern", jadeBool("plugin.minecraft.lectern", true))
                .onChanged { write("plugin.minecraft.lectern" to it) }

            val commandBlock by boolean("CommandBlock", jadeBool("plugin.minecraft.command_block", true))
                .onChanged { write("plugin.minecraft.command_block" to it) }

            /** Whether a TNT block is stable, and what would set it off. */
            val tntStability by boolean("TntStability", jadeBool("plugin.minecraft.tnt_stability", true))
                .onChanged { write("plugin.minecraft.tnt_stability" to it) }

            val waxed by boolean("Waxed", jadeBool("plugin.minecraft.waxed", true))
                .onChanged { write("plugin.minecraft.waxed" to it) }

            val playerHead by boolean("PlayerHead", jadeBool("plugin.minecraft.player_head", true))
                .onChanged { write("plugin.minecraft.player_head" to it) }

            val enchantmentPower by boolean(
                "EnchantmentPower", jadeBool("plugin.minecraft.enchantment_power", true)
            ).onChanged { write("plugin.minecraft.enchantment_power" to it) }

            private fun write(vararg pairs: Pair<String, Any>) = applyTo(jadeStore, JADE_ID, *pairs)
        }

        /** What Jade reports about entities. */
        object Entities : ValueGroup("Entities") {
            val health by boolean("Health", jadeBool("plugin.minecraft.entity_health", true))
                .onChanged { write("plugin.minecraft.entity_health" to it) }

            /** Print `12/20` instead of drawing twenty hearts. */
            val healthAsNumbers by boolean(
                "HealthAsNumbers", jadeBool("plugin.minecraft.entity_health\\.show_fractions", false)
            ).onChanged { write("plugin.minecraft.entity_health\\.show_fractions" to it) }

            /** Above this much health, switch to a number rather than a wall of icons. */
            val healthIconLimit by int(
                "HealthIconLimit", jadeInt("plugin.minecraft.entity_health\\.max_for_render", 40), 0..100
            ).onChanged { write("plugin.minecraft.entity_health\\.max_for_render" to it) }

            val healthIconsPerLine by int(
                "HealthIconsPerLine", jadeInt("plugin.minecraft.entity_health\\.icons_per_line", 10), 1..40
            ).onChanged { write("plugin.minecraft.entity_health\\.icons_per_line" to it) }

            val armor by boolean("Armor", jadeBool("plugin.minecraft.entity_armor", true))
                .onChanged { write("plugin.minecraft.entity_armor" to it) }

            val armorIconLimit by int(
                "ArmorIconLimit", jadeInt("plugin.minecraft.entity_armor\\.max_for_render", 20), 0..100
            ).onChanged { write("plugin.minecraft.entity_armor\\.max_for_render" to it) }

            val potionEffects by boolean("PotionEffects", jadeBool("plugin.minecraft.potion_effects", true))
                .onChanged { write("plugin.minecraft.potion_effects" to it) }

            val potionEffectLimit by int(
                "PotionEffectLimit", jadeInt("plugin.minecraft.potion_effects\\.limit", 7), 1..16
            ).onChanged { write("plugin.minecraft.potion_effects\\.limit" to it) }

            val mobGrowth by boolean("MobGrowth", jadeBool("plugin.minecraft.mob_growth", true))
                .onChanged { write("plugin.minecraft.mob_growth" to it) }

            val mobBreeding by boolean("MobBreeding", jadeBool("plugin.minecraft.mob_breeding", true))
                .onChanged { write("plugin.minecraft.mob_breeding" to it) }

            val animalOwner by boolean("AnimalOwner", jadeBool("plugin.minecraft.animal_owner", true))
                .onChanged { write("plugin.minecraft.animal_owner" to it) }

            val petArmor by enumChoice(
                "PetArmor",
                jadeEnum(JadePetArmor.entries, "plugin.minecraft.pet_armor", JadePetArmor.SHOW_DAMAGEABLE)
            ).onChanged { write("plugin.minecraft.pet_armor" to it.key) }

            val villagerProfession by boolean(
                "VillagerProfession", jadeBool("plugin.minecraft.villager_profession", true)
            ).onChanged { write("plugin.minecraft.villager_profession" to it) }

            /** How far along a zombie villager's cure is. */
            val zombieVillager by boolean(
                "ZombieVillager", jadeBool("plugin.minecraft.zombie_villager", true)
            ).onChanged { write("plugin.minecraft.zombie_villager" to it) }

            val horseStats by boolean("HorseStats", jadeBool("plugin.minecraft.horse_stats", true))
                .onChanged { write("plugin.minecraft.horse_stats" to it) }

            /** What a sheared or milked mob will give next, and when. */
            val nextDrop by boolean("NextDrop", jadeBool("plugin.minecraft.next_entity_drop", true))
                .onChanged { write("plugin.minecraft.next_entity_drop" to it) }

            val itemFrame by boolean("ItemFrame", jadeBool("plugin.minecraft.item_frame", true))
                .onChanged { write("plugin.minecraft.item_frame" to it) }

            val armorStand by boolean("ArmorStand", jadeBool("plugin.minecraft.armor_stand", true))
                .onChanged { write("plugin.minecraft.armor_stand" to it) }

            val painting by boolean("Painting", jadeBool("plugin.minecraft.painting", true))
                .onChanged { write("plugin.minecraft.painting" to it) }

            private fun write(vararg pairs: Pair<String, Any>) = applyTo(jadeStore, JADE_ID, *pairs)
        }

        /** Jade's accessibility options, including its screen reader. */
        object Accessibility : ValueGroup("Accessibility") {
            val textToSpeech by boolean(
                "TextToSpeech", jadeBool("accessibility.enableTextToSpeech", false)
            ).onChanged { write("accessibility.enableTextToSpeech" to it) }

            val textToSpeechMode by enumChoice(
                "TextToSpeechMode",
                jadeEnum(JadeTtsMode.entries, "accessibility.ttsMode", JadeTtsMode.TOGGLE)
            ).onChanged { write("accessibility.ttsMode" to it.key) }

            val narrateKeys by boolean("NarrateKeys", jadeBool("accessibility.narrateKeys", false))
                .onChanged { write("accessibility.narrateKeys" to it) }

            /** Mirror the readout for a left-handed layout. */
            val flipMainHand by boolean("FlipMainHand", jadeBool("accessibility.flipMainHand", false))
                .onChanged { write("accessibility.flipMainHand" to it) }

            val textBackgroundOpacity by float(
                "TextBackgroundOpacity", jadeFloat("accessibility.textBackgroundOpacity", 0f), 0f..1f
            ).onChanged { write("accessibility.textBackgroundOpacity" to it) }

            private fun write(vararg pairs: Pair<String, Any>) = applyTo(jadeStore, JADE_ID, *pairs)
        }
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
    /**
     * Sodium Extra. Keys verified against a real `sodium-extra-options.json`;
     * the two enums against `SodiumExtraGameOptions$OverlayCorner` and
     * `$TextContrast` in the jar, because a config only shows the constant
     * currently selected.
     *
     * This is the mod carrying the FPS readout, the animation and particle
     * switches and the toast filter - the settings a player actually reaches
     * for. It was bundled without a bridge, so until now all of it lived in
     * Sodium's own video-settings screen.
     *
     * The fog block is deliberately not here. Its `dimension_overrides` and
     * `protected_gameplay` sections are per-dimension maps rather than flat
     * values, and `CustomAmbience` already owns fog in this client.
     */
    object SodiumExtra : ValueGroup("SodiumExtra") {

        init {
            treeAll(Animations, Particles, Details, Render, Overlay)
        }

        /** Texture animation. Turning these off is a real gain on weak GPUs. */
        object Animations : ValueGroup("Animations") {
            val all by boolean("All", seBool("animation_settings.animation", true))
                .onChanged { seWrite("animation_settings.animation" to it) }

            val water by boolean("Water", seBool("animation_settings.water", true))
                .onChanged { seWrite("animation_settings.water" to it) }

            val lava by boolean("Lava", seBool("animation_settings.lava", true))
                .onChanged { seWrite("animation_settings.lava" to it) }

            val fire by boolean("Fire", seBool("animation_settings.fire", true))
                .onChanged { seWrite("animation_settings.fire" to it) }

            val portal by boolean("Portal", seBool("animation_settings.portal", true))
                .onChanged { seWrite("animation_settings.portal" to it) }

            val blocks by boolean("Blocks", seBool("animation_settings.block_animations", true))
                .onChanged { seWrite("animation_settings.block_animations" to it) }

            val sculkSensor by boolean("SculkSensor", seBool("animation_settings.sculk_sensor", true))
                .onChanged { seWrite("animation_settings.sculk_sensor" to it) }
        }

        object Particles : ValueGroup("Particles") {
            val all by boolean("All", seBool("particle_settings.particles", true))
                .onChanged { seWrite("particle_settings.particles" to it) }

            val rainSplash by boolean("RainSplash", seBool("particle_settings.rain_splash", true))
                .onChanged { seWrite("particle_settings.rain_splash" to it) }

            val blockBreak by boolean("BlockBreak", seBool("particle_settings.block_break", true))
                .onChanged { seWrite("particle_settings.block_break" to it) }

            val blockBreaking by boolean("BlockBreaking", seBool("particle_settings.block_breaking", true))
                .onChanged { seWrite("particle_settings.block_breaking" to it) }
        }

        /** Sky and world detail. */
        object Details : ValueGroup("Details") {
            val sky by boolean("Sky", seBool("detail_settings.sky", true))
                .onChanged { seWrite("detail_settings.sky" to it) }

            val sun by boolean("Sun", seBool("detail_settings.sun", true))
                .onChanged { seWrite("detail_settings.sun" to it) }

            val moon by boolean("Moon", seBool("detail_settings.moon", true))
                .onChanged { seWrite("detail_settings.moon" to it) }

            val stars by boolean("Stars", seBool("detail_settings.stars", true))
                .onChanged { seWrite("detail_settings.stars" to it) }

            val rainSnow by boolean("RainSnow", seBool("detail_settings.rain_snow", true))
                .onChanged { seWrite("detail_settings.rain_snow" to it) }

            val biomeColors by boolean("BiomeColors", seBool("detail_settings.biome_colors", true))
                .onChanged { seWrite("detail_settings.biome_colors" to it) }

            val skyColors by boolean("SkyColors", seBool("detail_settings.sky_colors", true))
                .onChanged { seWrite("detail_settings.sky_colors" to it) }
        }

        /** Per-entity and per-block-entity rendering. */
        object Render : ValueGroup("Render") {
            val lightUpdates by boolean("LightUpdates", seBool("render_settings.light_updates", true))
                .onChanged { seWrite("render_settings.light_updates" to it) }

            val itemFrame by boolean("ItemFrame", seBool("render_settings.item_frame", true))
                .onChanged { seWrite("render_settings.item_frame" to it) }

            val armorStand by boolean("ArmorStand", seBool("render_settings.armor_stand", true))
                .onChanged { seWrite("render_settings.armor_stand" to it) }

            val painting by boolean("Painting", seBool("render_settings.painting", true))
                .onChanged { seWrite("render_settings.painting" to it) }

            val piston by boolean("Piston", seBool("render_settings.piston", true))
                .onChanged { seWrite("render_settings.piston" to it) }

            val beaconBeam by boolean("BeaconBeam", seBool("render_settings.beacon_beam", true))
                .onChanged { seWrite("render_settings.beacon_beam" to it) }

            /** Stops a beacon beam being drawn all the way to the build limit. */
            val limitBeaconBeamHeight by boolean(
                "LimitBeaconBeamHeight",
                seBool("render_settings.limit_beacon_beam_height", false)
            ).onChanged { seWrite("render_settings.limit_beacon_beam_height" to it) }

            val enchantingTableBook by boolean(
                "EnchantingTableBook",
                seBool("render_settings.enchanting_table_book", true)
            ).onChanged { seWrite("render_settings.enchanting_table_book" to it) }

            val itemFrameNameTag by boolean(
                "ItemFrameNameTag",
                seBool("render_settings.item_frame_name_tag", true)
            ).onChanged { seWrite("render_settings.item_frame_name_tag" to it) }

            val playerNameTag by boolean("PlayerNameTag", seBool("render_settings.player_name_tag", true))
                .onChanged { seWrite("render_settings.player_name_tag" to it) }
        }

        /** The on-screen readouts and the toast filter. */
        object Overlay : ValueGroup("Overlay") {
            val showFps by boolean("ShowFps", seBool("extra_settings.show_fps", false))
                .onChanged { seWrite("extra_settings.show_fps" to it) }

            /** The longer FPS line, with the minimum and average beside the current. */
            val extendedFps by boolean("ExtendedFps", seBool("extra_settings.show_f_p_s_extended", true))
                .onChanged { seWrite("extra_settings.show_f_p_s_extended" to it) }

            val showCoords by boolean("ShowCoords", seBool("extra_settings.show_coords", false))
                .onChanged { seWrite("extra_settings.show_coords" to it) }

            val corner by enumChoice(
                "Corner",
                seEnum(SodiumExtraCorner.entries, "extra_settings.overlay_corner", SodiumExtraCorner.TOP_LEFT)
            ).onChanged { seWrite("extra_settings.overlay_corner" to it.key) }

            val textContrast by enumChoice(
                "TextContrast",
                seEnum(SodiumExtraContrast.entries, "extra_settings.text_contrast", SodiumExtraContrast.NONE)
            ).onChanged { seWrite("extra_settings.text_contrast" to it.key) }

            val toasts by boolean("Toasts", seBool("extra_settings.toasts", true))
                .onChanged { seWrite("extra_settings.toasts" to it) }

            val advancementToast by boolean("AdvancementToast", seBool("extra_settings.advancement_toast", true))
                .onChanged { seWrite("extra_settings.advancement_toast" to it) }

            val recipeToast by boolean("RecipeToast", seBool("extra_settings.recipe_toast", true))
                .onChanged { seWrite("extra_settings.recipe_toast" to it) }

            val systemToast by boolean("SystemToast", seBool("extra_settings.system_toast", true))
                .onChanged { seWrite("extra_settings.system_toast" to it) }

            val tutorialToast by boolean("TutorialToast", seBool("extra_settings.tutorial_toast", true))
                .onChanged { seWrite("extra_settings.tutorial_toast" to it) }

            /** Stops the F3 screen rewriting itself every frame. */
            val steadyDebugHud by boolean("SteadyDebugHud", seBool("extra_settings.steady_debug_hud", true))
                .onChanged { seWrite("extra_settings.steady_debug_hud" to it) }

            val steadyDebugHudInterval by int(
                "SteadyDebugHudInterval",
                seInt("extra_settings.steady_debug_hud_refresh_interval", 1),
                1..20
            ).onChanged { seWrite("extra_settings.steady_debug_hud_refresh_interval" to it) }

            val cloudHeightOverride by boolean(
                "CloudHeightOverride",
                seBool("extra_settings.cloud_height_override", false)
            ).onChanged { seWrite("extra_settings.cloud_height_override" to it) }

            val cloudHeight by int("CloudHeight", seInt("extra_settings.cloud_height", 192), 0..320)
                .onChanged { seWrite("extra_settings.cloud_height" to it) }

            /** Removes the sneak animation delay. Visual only. */
            val instantSneak by boolean("InstantSneak", seBool("extra_settings.instant_sneak", false))
                .onChanged { seWrite("extra_settings.instant_sneak" to it) }

            val adaptiveSync by boolean("AdaptiveSync", seBool("extra_settings.use_adaptive_sync", false))
                .onChanged { seWrite("extra_settings.use_adaptive_sync" to it) }

            /** Blocks a server's own shader packs from being applied. */
            val preventShaders by boolean("PreventShaders", seBool("extra_settings.prevent_shaders", false))
                .onChanged { seWrite("extra_settings.prevent_shaders" to it) }
        }
    }

    /**
     * AppleSkin. Keys verified against a real `appleskin.json5`.
     *
     * This module's own notes used to say AppleSkin "stores its settings
     * through NeoForge's TOML config spec, which this bridge does not speak".
     * That is wrong, and it is why the mod went unbridged: the Fabric build
     * writes plain JSON with `//` comments, and Gson reads comments in the
     * lenient mode `JsonParser` already uses. Checked by parsing the real file
     * with the same call `JsonConfigStore` makes - nine keys, all readable.
     *
     * Rewriting the file drops upstream's comments, which AppleSkin restores
     * the next time it saves. It never fails to read its own file for want of
     * them.
     */
    object AppleSkin : ValueGroup("AppleSkin") {
        private val store = ModConfigStore.json("appleskin.json5")

        /** Hunger and saturation numbers on a food item's tooltip, on shift. */
        val foodTooltip by boolean("FoodTooltip", store.readBoolean("showFoodValuesInTooltip") ?: true)
            .onChanged { write("showFoodValuesInTooltip" to it) }

        /** The same tooltip without having to hold shift. */
        val foodTooltipAlways by boolean(
            "FoodTooltipAlways",
            store.readBoolean("showFoodValuesInTooltipAlways") ?: true
        ).onChanged { write("showFoodValuesInTooltipAlways" to it) }

        /** Saturation drawn over the hunger bar. The headline feature. */
        val saturationOverlay by boolean(
            "SaturationOverlay",
            store.readBoolean("showSaturationHudOverlay") ?: true
        ).onChanged { write("showSaturationHudOverlay" to it) }

        /** What the food you are holding would restore, previewed on the bar. */
        val heldFoodPreview by boolean(
            "HeldFoodPreview",
            store.readBoolean("showFoodValuesHudOverlay") ?: true
        ).onChanged { write("showFoodValuesHudOverlay" to it) }

        val previewFromOffhand by boolean(
            "PreviewFromOffhand",
            store.readBoolean("showFoodValuesHudOverlayWhenOffhand") ?: true
        ).onChanged { write("showFoodValuesHudOverlayWhenOffhand" to it) }

        /** Exhaustion as a bar behind the hunger row - how close the next tick is. */
        val exhaustionUnderlay by boolean(
            "ExhaustionUnderlay",
            store.readBoolean("showFoodExhaustionHudUnderlay") ?: true
        ).onChanged { write("showFoodExhaustionHudUnderlay" to it) }

        /** Estimated health the held food would restore, on the health bar. */
        val healthPreview by boolean(
            "HealthPreview",
            store.readBoolean("showFoodHealthHudOverlay") ?: true
        ).onChanged { write("showFoodHealthHudOverlay" to it) }

        /** Whether the preview icons shake along with vanilla's own animation. */
        val vanillaAnimations by boolean(
            "VanillaAnimations",
            store.readBoolean("showVanillaAnimationsOverlay") ?: true
        ).onChanged { write("showVanillaAnimationsOverlay" to it) }

        /** How visible the flashing preview icons get at their peak. */
        val flashAlpha by float("FlashAlpha", store.readFloat("maxHudOverlayFlashAlpha") ?: 0.65f, 0f..1f)
            .onChanged { write("maxHudOverlayFlashAlpha" to it) }

        private fun write(pair: Pair<String, Any>) = applyTo(store, "appleskin", pair)
    }

    internal fun applyTo(store: ModConfigStore, modId: String, vararg pairs: Pair<String, Any>) {
        if (!ModConfigStore.isModLoaded(modId)) {
            chat("§7$modId is not installed, so that setting has nothing to change.")
            return
        }

        store.write(pairs.toMap())
        logger.info("Wrote ${pairs.size} value(s) to the $modId config")
        chat("§7Saved to $modId. It applies the next time the game starts.")
    }
}

private const val JADE_ID = "jade"

/**
 * Jade's config, which lives in a subdirectory rather than beside the others.
 *
 * Top level rather than a member of [ModuleBundledMods.Jade] on purpose: the nested
 * groups read it while their own objects are being constructed, which happens inside
 * the enclosing object's own construction. Reaching back into a half-built object
 * there is how this becomes a null at startup rather than a compile error.
 */
private val jadeStore = ModConfigStore.json("jade/jade.json")

private fun jadeBool(key: String, fallback: Boolean) = jadeStore.readBoolean(key) ?: fallback

private fun jadeInt(key: String, fallback: Int) = jadeStore.readInt(key) ?: fallback

private fun jadeFloat(key: String, fallback: Float) = jadeStore.readFloat(key) ?: fallback

/**
 * Reads a stored enum, matching on the name Jade writes rather than the ClickGUI label.
 *
 * An unknown value falls back rather than throwing: Jade may add a constant in a later
 * version, and a config this client cannot parse is not a reason to refuse to start.
 */
private fun <T> jadeEnum(entries: List<T>, key: String, fallback: T): T
    where T : Enum<T>, T : ConfigKeyed =
    jadeStore.readString(key)?.let { raw -> entries.firstOrNull { it.key == raw } } ?: fallback

/**
 * A choice whose ClickGUI label and the mod's own config value differ.
 *
 * Jade and Sodium Extra both store enum constants verbatim - `HOLD_KEY`,
 * `BOTTOM_RIGHT` - which are accurate and unreadable. [tag] is what the player
 * sees; [key] is what is written to the mod's config.
 */
interface ConfigKeyed {
    val key: String
}

/** `IWailaConfig.DisplayMode`. */
enum class JadeDisplayMode(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    /** Press the key to turn the readout on and off. */
    TOGGLE("Toggle", "TOGGLE"),

    /** The readout is only shown while the key is held. */
    HOLD("Hold", "HOLD_KEY"),

    /** Always on, but cut down until you hold the details key. */
    LITE("Lite", "LITE")
}

/** `IWailaConfig.FluidMode` - whether a fluid counts as a block to look at. */
enum class JadeFluidMode(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    NONE("None", "NONE"),
    ANY("Any", "ANY"),

    /** Only when there is no solid block behind it. */
    FALLBACK("Fallback", "FALLBACK")
}

/** `IWailaConfig.PerspectiveMode` - where the trace starts from. */
enum class JadePerspective(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    CAMERA("Camera", "CAMERA"),
    EYE("Eye", "EYE")
}

/** `IWailaConfig.IconMode` - where the block or entity icon sits in the box. */
enum class JadeIconMode(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    TOP("Top", "TOP"),
    CENTERED("Centered", "CENTERED"),
    INLINE("Inline", "INLINE"),
    HIDE("Hide", "HIDE")
}

/** `IWailaConfig.TTSMode`. */
enum class JadeTtsMode(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    TOGGLE("Toggle", "TOGGLE"),
    PRESS("Press", "PRESS")
}

/** `IWailaConfig.BossBarOverlapMode` - what gives way when both want the same space. */
enum class JadeBossBar(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    NOTHING("Nothing", "NO_OPERATION"),
    HIDE_BOSS_BAR("HideBossBar", "HIDE_BOSS_BAR"),
    HIDE_TOOLTIP("HideTooltip", "HIDE_TOOLTIP"),
    PUSH_DOWN("PushDown", "PUSH_DOWN")
}

/** `ModNameProvider.Mode`. */
enum class JadeModName(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    ON("On", "ON"),
    OFF("Off", "OFF"),

    /** Shown, but in a smaller type than the object name. */
    SMALLER("Smaller", "SMALLER")
}

/** `RegistryNameProvider.Mode`. */
enum class JadeRegistryName(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    ON("On", "ON"),
    OFF("Off", "OFF"),

    /** Follow the vanilla advanced-tooltips setting (F3+H). */
    ADVANCED_TOOLTIPS("AdvancedTooltips", "ADVANCED_TOOLTIPS")
}

/** `PetArmorProvider.Mode`. */
enum class JadePetArmor(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    OFF("Off", "OFF"),
    SHOW_ALL("ShowAll", "SHOW_ALL"),

    /** Only armour that can actually take damage, which is the useful half. */
    SHOW_DAMAGEABLE("ShowDamageable", "SHOW_DAMAGEABLE")
}

private const val SODIUM_EXTRA_ID = "sodium-extra"

/**
 * Sodium Extra's config store and helpers.
 *
 * Top level rather than members of [ModuleBundledMods.SodiumExtra], for the same
 * reason as the Jade helpers above: the nested groups read it while their own
 * objects are being constructed, which happens inside the enclosing object's
 * construction. Reaching back into a half-built object there gives a null at
 * startup rather than a compile error.
 */
private val sodiumExtraStore = ModConfigStore.json("sodium-extra-options.json")

private fun seBool(key: String, fallback: Boolean) = sodiumExtraStore.readBoolean(key) ?: fallback

private fun seInt(key: String, fallback: Int) = sodiumExtraStore.readInt(key) ?: fallback

/** Matches on the constant Sodium Extra writes, falling back rather than throwing. */
private fun <T> seEnum(entries: List<T>, key: String, fallback: T): T where T : Enum<T>, T : ConfigKeyed {
    val stored = sodiumExtraStore.readString(key) ?: return fallback
    return entries.firstOrNull { it.key == stored } ?: fallback
}

private fun seWrite(pair: Pair<String, Any>) =
    ModuleBundledMods.applyTo(sodiumExtraStore, SODIUM_EXTRA_ID, pair)

/** `SodiumExtraGameOptions$OverlayCorner`, read from the jar. */
enum class SodiumExtraCorner(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    TOP_LEFT("TopLeft", "TOP_LEFT"),
    TOP_RIGHT("TopRight", "TOP_RIGHT"),
    BOTTOM_LEFT("BottomLeft", "BOTTOM_LEFT"),
    BOTTOM_RIGHT("BottomRight", "BOTTOM_RIGHT")
}

/** `SodiumExtraGameOptions$TextContrast`, read from the jar. */
enum class SodiumExtraContrast(override val tag: String, override val key: String) : Tagged, ConfigKeyed {
    NONE("None", "NONE"),
    BACKGROUND("Background", "BACKGROUND"),
    SHADOW("Shadow", "SHADOW")
}
