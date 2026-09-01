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

package net.ccbluex.liquidbounce.render.colorgrading;

/**
 * One frame's worth of grading, in the order the shader's uniform block expects.
 *
 * @param saturation  1.0 leaves colour alone, 0.0 is greyscale, above 1.0 pushes
 * @param vibrance    0.0 is off; boosts only the pixels that are still close to grey
 * @param contrast    1.0 leaves it alone; pivots on mid grey, not on black
 * @param brightness  a plain multiplier, 1.0 is untouched
 * @param gamma       1.0 is untouched; higher lifts the shadows
 * @param temperature 0.0 is neutral; positive is warmer, negative cooler
 * @param tint        0.0 is neutral; positive pushes green, negative magenta
 */
public record ColorGradingConfig(
        float saturation,
        float vibrance,
        float contrast,
        float brightness,
        float gamma,
        float temperature,
        float tint
) {

    /** Every term at its no-op value. The engine skips the pass entirely on this. */
    public static final ColorGradingConfig IDENTITY =
            new ColorGradingConfig(1f, 0f, 1f, 1f, 1f, 0f, 0f);

    /**
     * Whether running the pass would change a single pixel.
     *
     * Worth checking because the alternative is a full-screen draw and a buffer
     * upload every frame to produce the frame that was already there.
     */
    public boolean isIdentity() {
        return saturation == 1f
                && vibrance == 0f
                && contrast == 1f
                && brightness == 1f
                && gamma == 1f
                && temperature == 0f
                && tint == 0f;
    }
}
