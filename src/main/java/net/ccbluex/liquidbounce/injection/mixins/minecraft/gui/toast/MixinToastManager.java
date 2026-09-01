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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.toast;

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleToastControl;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops a toast before it is queued, when the player has switched its kind off.
 *
 * Cancelling at the queue rather than at the draw means a suppressed toast never
 * occupies one of the four slots, so an advancement spam does not push a system
 * warning off the screen while it is being hidden.
 *
 * @see ModuleToastControl
 */
@Mixin(ToastManager.class)
public class MixinToastManager {

    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void tsunami$filterToast(Toast toast, CallbackInfo ci) {
        if (!ModuleToastControl.INSTANCE.shouldShow(toast)) {
            ci.cancel();
        }
    }
}
