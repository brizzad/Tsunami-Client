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

/**
 * Flat dropped items, merged from beamingblue's Flat Items.
 *
 * <h2>Origin and licence</h2>
 *
 * <p>The rendering in this package is derived from
 * <a href="https://github.com/beamingblue/flat-items">Flat Items</a> by beamingblue,
 * licensed under the MIT License, which is itself a rewrite of Noryea's Fast Items.
 * MIT permits inclusion in a GPLv3 work, which is what this is; attribution is
 * preserved here as that permission requires.
 *
 * <h2>Why the source was merged rather than the jar bundled</h2>
 *
 * <p>The whole mod is two mixins and a settings interface. Bundling it would put a
 * fourteenth jar in the load order and a second settings screen in front of the
 * player - upstream configures itself through Mod Menu, which Tsunami deliberately
 * does not ship. The two mixins were taken and the settings interface was left
 * behind:
 * {@link net.ccbluex.liquidbounce.features.module.modules.render.ModuleFlatItems}
 * supplies those values from the ClickGUI instead, which also gets the feature a
 * keybind and a place in profile export.
 *
 * <h2>What changed in the merge</h2>
 *
 * <ul>
 *   <li>{@code FlatItems.settings()} and its {@code Settings} interface are gone.
 *       The module is read directly, and the module's own {@code running} flag
 *       replaces upstream's {@code enabled} setting - a Tsunami module that is off
 *       is already not running.</li>
 *   <li>The billboard maths moved out of the {@code Settings.Facing} enum and into
 *       the module, so the mixin holds no configuration of its own.</li>
 *   <li>The duck interface was renamed to the fork's convention,
 *       {@code liquid_bounce$quads} on
 *       {@link net.ccbluex.liquidbounce.interfaces.ItemStackRenderStateAddition}.</li>
 *   <li>Upstream's Fabric, NeoForge and Mod Menu entry points were dropped. This
 *       fork is Fabric-only and has its own configuration UI.</li>
 * </ul>
 *
 * <h2>What this does not touch</h2>
 *
 * <p>Only the orientation and the quad list of an already-dropped item's model. What
 * the item is, where it lies, when it despawns and whether you can pick it up are
 * untouched, and the item entity itself is not modified. With the module off, the
 * vanilla rotation is applied exactly as before.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.flatitems;
