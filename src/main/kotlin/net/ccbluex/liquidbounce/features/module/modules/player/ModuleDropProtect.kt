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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack

/**
 * DropProtect
 *
 * Ignores the drop key while you are holding something worth keeping.
 *
 * Q sits next to the movement keys and the drop is instant and silent, so the
 * failure mode is throwing a pickaxe into lava mid-panic and only finding out
 * afterwards. This refuses the keypress for the categories you mark, and says
 * so in chat so a refused drop is never mistaken for a dropped item.
 *
 * ## What it covers, and what it does not
 *
 * This is the hotbar drop key. Dragging a stack out of an open inventory is a
 * deliberate two-part gesture with the item visible in the cursor the whole
 * time, so it is not the accident this protects against and is left alone.
 *
 * Nothing here touches the server. A refused drop is a keypress the client
 * never acts on, which is the same as not having pressed it.
 */
object ModuleDropProtect : ClientModule("DropProtect", ModuleCategories.PLAYER) {

    /** Anything with an enchantment on it. */
    private val enchanted by boolean("Enchanted", true)

    /** Anything the game marks as better than common - the tooltip colours. */
    private val rareItems by boolean("RareItems", true)

    /** Armour, tools, weapons - anything with a durability bar. */
    private val damageable by boolean("Damageable", false)

    /**
     * Anything named on an anvil. A renamed item is one somebody decided was
     * worth naming, which is as good a signal as the game gives.
     */
    private val renamed by boolean("Renamed", true)

    /** Say in chat when a drop was refused, so silence never means "dropped". */
    private val notify by boolean("Notify", true)

    /** Chat spam guard: at most one refusal notice per this many milliseconds. */
    private const val NOTIFY_COOLDOWN_MS = 2000L

    private var lastNotifiedAt = 0L

    @Suppress("unused")
    private val keyHandler = handler<KeybindIsPressedEvent> { event ->
        if (!event.isPressed || event.keyBinding !== mc.options.keyDrop) {
            return@handler
        }

        val held = mc.player?.mainHandItem ?: return@handler
        if (held.isEmpty || !isProtected(held)) {
            return@handler
        }

        event.isPressed = false

        if (!notify) {
            return@handler
        }

        val now = System.currentTimeMillis()
        if (now - lastNotifiedAt < NOTIFY_COOLDOWN_MS) {
            return@handler
        }
        lastNotifiedAt = now

        chat("Held ${held.hoverName.string} is protected - drop refused.", this)
    }

    private fun isProtected(stack: ItemStack): Boolean {
        if (enchanted && stack.isEnchanted) {
            return true
        }

        if (rareItems && stack.rarity.ordinal > 0) {
            return true
        }

        if (damageable && stack.isDamageableItem) {
            return true
        }

        // A custom name is the only "the player cared about this" flag the item
        // carries, and it survives being moved between inventories.
        return renamed && stack.has(DataComponents.CUSTOM_NAME)
    }

}
