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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * ArmorHud
 *
 * Your four armour pieces and their durability, drawn beside the hotbar where
 * you can see them without opening the inventory.
 *
 * Merged from uku's Armor HUD (https://modrinth.com/mod/ukus-armor-hud), MIT,
 * which is the most-installed mod in this niche by a wide margin. The layout
 * maths, the offhand and attack-indicator spacing, and the durability warning
 * rule are upstream's; this class is the ClickGUI face and
 * [net.ccbluex.liquidbounce.features.armorhud.ArmorHudRenderer] is the drawing.
 *
 * ## Overlaps with the theme HUD component
 *
 * The fork already ships an armour readout as a theme HUD component. This is a
 * second, different one: it draws on the vanilla HUD in the vanilla style,
 * anchored to the hotbar, rather than as a themed panel. Off by default for
 * exactly that reason - turning both on gives you two armour displays, which is
 * a choice rather than a default.
 *
 * ## Not merged
 *
 * Upstream's bobbing warning sprite and its armour-breaking sound need bundled
 * assets; the warning here is a tint on the slot instead. Its `TOP_CENTER`
 * anchor and the `DAMAGED_PIECES` filter were kept; the rest of its
 * permutations were not, and can be added if anyone misses them.
 */
object ModuleArmorHud : ClientModule("ArmorHud", ModuleCategories.RENDER) {

    enum class Anchor(override val tag: String) : Tagged {
        /** Beside the hotbar, clearing the offhand slot and attack indicator. */
        HOTBAR("Hotbar"),
        TOP("Top"),
        TOP_CENTER("TopCenter"),
        BOTTOM("Bottom");

        val isTop: Boolean get() = this == TOP || this == TOP_CENTER
    }

    enum class Side(override val tag: String) : Tagged {
        LEFT("Left"),
        RIGHT("Right");

        val opposite: Side get() = if (this == LEFT) RIGHT else LEFT
    }

    enum class Orientation(override val tag: String) : Tagged {
        HORIZONTAL("Horizontal"),
        VERTICAL("Vertical")
    }

    enum class Style(override val tag: String) : Tagged {
        /** The vanilla hotbar sprite, stretched to the widget. */
        HOTBAR("Hotbar"),
        ROUNDED("Rounded"),
        NONE("None")
    }

    enum class WidgetShown(override val tag: String) : Tagged {
        ALWAYS("Always"),

        /** Nothing at all until at least one piece is worn. */
        IF_ANY_PRESENT("IfAnyPresent"),

        /** Only the slots actually filled, so the widget shrinks. */
        NOT_EMPTY("NotEmpty"),

        /** Only pieces low enough to be worth replacing. */
        DAMAGED_PIECES("DamagedPieces")
    }

    enum class OffhandBehavior(override val tag: String) : Tagged {
        /** Move over only when the offhand or attack indicator is really there. */
        ADHERE("Adhere"),

        /** Always keep the gap, so the widget never shifts mid-fight. */
        ALWAYS_LEAVE_SPACE("AlwaysLeaveSpace"),

        ALWAYS_IGNORE("AlwaysIgnore")
    }

    enum class DurabilityDisplay(override val tag: String) : Tagged {
        /** Vanilla's own damage bar on the item, and nothing else. */
        BAR("Bar"),
        NUMERIC("Numeric"),
        PERCENTAGE("Percentage")
    }

    val anchor by enumChoice("Anchor", Anchor.HOTBAR)
    val side by enumChoice("Side", Side.LEFT)
    val orientation by enumChoice("Orientation", Orientation.HORIZONTAL)
    val style by enumChoice("Style", Style.HOTBAR)
    val widgetShown by enumChoice("WidgetShown", WidgetShown.IF_ANY_PRESENT)
    val offhandBehavior by enumChoice("OffhandBehavior", OffhandBehavior.ADHERE)
    val durabilityDisplay by enumChoice("DurabilityDisplay", DurabilityDisplay.BAR)

    /** Helmet last instead of first. */
    val reversed by boolean("Reversed", false)

    /** Draw the empty-slot silhouettes vanilla uses in the inventory. */
    val showIcons by boolean("ShowIcons", true)

    val offsetX by int("OffsetX", 0, -256..256)
    val offsetY by int("OffsetY", 0, -256..256)

    /** Tint a piece once it is close enough to breaking to matter. */
    val warnOnLowDurability by boolean("WarnOnLowDurability", true)
    val warningColor by color("WarningColor", Color4b(0xFF, 0x3B, 0x30, 0x80))

    /** Warn below this fraction of durability remaining. */
    val warnBelowPercent by int("WarnBelowPercent", 10, 1..90)

    /** Or below this many points, whichever triggers first. */
    val warnBelowDurability by int("WarnBelowDurability", 20, 0..500)
}
