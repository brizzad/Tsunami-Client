/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 * ---------------------------------------------------------------------------
 * Derived from Natural Motion Blur (https://github.com/ItsPasi/natural-motionblur-fabric),
 * originally licensed LGPL-3.0-only and relicensed here under GPL-3.0 as
 * permitted by LGPL-3.0 section 2. The upstream in-game config/keybind/command
 * layer is replaced by ModuleMotionBlur, which drives this holder.
 * ---------------------------------------------------------------------------
 */
package net.ccbluex.liquidbounce.render.motionblur;

import java.util.function.BooleanSupplier;

/**
 * Plain mutable settings holder read by the motion blur engine every frame.
 *
 * <p>Upstream kept these in a JSON file managed by its own config screen. In Tsunami the
 * single source of truth is {@code ModuleMotionBlur}, which pushes its ClickGUI values in
 * through {@link #get()}. Nothing here is persisted - the module's own value system owns
 * serialization, so this stays a dumb frame-local mirror.
 */
public class MotionBlurConfig {

    private static final MotionBlurConfig INSTANCE = new MotionBlurConfig();

    public static MotionBlurConfig get() {
        return INSTANCE;
    }

    /**
     * The module owns this state. Pulling it through a supplier rather than mirroring it into
     * a field means it can never go stale: ModuleMotionBlur has no disable hook to push a final
     * false from, because onToggled is final on ClientModule and a module's event handlers stop
     * firing the moment it stops running.
     */
    private static BooleanSupplier enabledSupplier = () -> false;

    /** Called once at module registration to point this at the module's running state. */
    public static void bindEnabled(BooleanSupplier supplier) {
        enabledSupplier = supplier;
    }

    public boolean isEnabled() {
        return enabledSupplier.getAsBoolean();
    }

    /** Scales blur up when FPS outruns the monitor, so the effect stays perceptually stable. */
    public boolean refreshRateScaling = true;

    public float motionBlurStrength = 1.0F;

    public BlurAlgorithm blurAlgorithm = BlurAlgorithm.VELOCITY_BASED;

    public enum BlurAlgorithm {
        VELOCITY_BASED, FRAME_BLENDING, HYBRID_BLENDING, ACCUMULATION_MAX, ACCUMULATION_MIX
    }

    public boolean usesVelocityBlur() {
        return blurAlgorithm == BlurAlgorithm.VELOCITY_BASED || blurAlgorithm == BlurAlgorithm.HYBRID_BLENDING;
    }

    public boolean allowsRefreshRateScaling() {
        return blurAlgorithm == BlurAlgorithm.VELOCITY_BASED;
    }

    /** Hybrid blending derives its own strength, so the slider is pinned while it is active. */
    public boolean locksStrengthToOne() {
        return blurAlgorithm == BlurAlgorithm.HYBRID_BLENDING;
    }

    public float getEffectiveMotionBlurStrength() {
        return locksStrengthToOne() ? 1.0F : motionBlurStrength;
    }
}
