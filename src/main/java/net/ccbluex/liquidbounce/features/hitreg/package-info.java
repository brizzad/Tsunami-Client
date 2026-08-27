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
 * Early hit feedback, merged from jasspir's BetterHitreg.
 *
 * <h2>Origin and licence</h2>
 *
 * The hit classification in this package is derived from
 * <a href="https://github.com/jasspir/BetterHitreg">BetterHitreg</a> by Jass,
 * licensed under the Apache License 2.0. Apache-2.0 permits inclusion in a
 * GPLv3 work, which is what this is; the original licence and attribution are
 * preserved here as that permission requires. Upstream is credited in the
 * client's README and on every module description that depends on this code.
 *
 * <h2>Why the source was merged rather than the jar bundled</h2>
 *
 * A bundled jar carries its own settings screen, its own config file and its
 * own keybinds. Everything in Tsunami is configured in one place, so the
 * pieces that decide <em>what a hit is</em> were taken and the pieces that
 * decide <em>how you configure it</em> were left behind: {@code ui/},
 * {@code settings/} and {@code Commands} have no equivalent here, and
 * {@link net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterHitreg}
 * supplies their values from the ClickGUI instead.
 *
 * <h2>What was deliberately not merged</h2>
 *
 * BetterHitreg is a fight-analysis tool as well as a feedback mod. These parts
 * were dropped as out of scope for Tsunami rather than for any technical
 * reason:
 *
 * <ul>
 *   <li><b>Metronome</b> - a periodic click to time attacks by. Tsunami's scope
 *       rule excludes timing aids, and this is one.</li>
 *   <li><b>Ghost, delay, inconsistency and jump-reset chat alerts</b>, and the
 *       per-fight accuracy summary - combat analytics, not on the approved
 *       feature list.</li>
 *   <li><b>Target glow on perfect hits and jump resets</b> - same reason, and
 *       it fights with the client's own target rendering.</li>
 *   <li><b>Sound muffling, sharpening and silencing</b>, and the hiding of
 *       nearby fights - audio and render preferences unrelated to hit
 *       registration.</li>
 *   <li><b>{@code MultiVersion}</b> - upstream's own protocol shim. This client
 *       already has one, so the handful of helpers actually needed were
 *       inlined into {@link net.ccbluex.liquidbounce.features.hitreg.HitState}
 *       instead.</li>
 * </ul>
 *
 * <h2>What this does not touch</h2>
 *
 * The attack packet, its timing and its contents are untouched, as is whether
 * a swing is sent at all. Reach, hit detection and cooldown are the server's
 * to decide and still are. The only change is that the client stops waiting
 * for the server's verdict before drawing the flash and playing the sound,
 * which is upstream's own description of itself: appearance only, actual hits
 * unmodified.
 */
package net.ccbluex.liquidbounce.features.hitreg;
