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
 */

package net.ccbluex.liquidbounce.integration.theme.component

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.input.Bindable
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.render.Alignment
import java.util.UUID

/**
 * Represents a HUD component
 */
abstract class HudComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment = Alignment.center(),
    val tweaks: Array<HudComponentTweak> = emptyArray(),
    val componentDescription: String = "",
) : ToggleableValueGroup(parent = ModuleHud, name = name, enabled = enabled), Bindable {

    val id: UUID = UUID.randomUUID()

    /**
     * The key that toggles this component.
     *
     * Components did not have one until the native HUD elements became draggable.
     * Several of them used to be modules, and a module carries a bind - so without
     * this, making an element movable would have quietly taken its key away.
     *
     * Unbound by default, and excluded from an exported config the same way a
     * module's bind is, because a shared config should not rebind someone's
     * keyboard.
     */
    internal val bindValue = bind(
        "Bind",
        InputBind(InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.value, InputBind.BindAction.TOGGLE),
    ).doNotIncludeWhen { !AutoConfig.includeConfiguration.includeBinds }
        .independentDescription()

    override val bind get() = bindValue.get()

    /**
     * Why this component means nothing on the negotiated protocol, or null when it
     * applies everywhere.
     *
     * The same hook `ClientModule` carries, for the same reason and with the same rule:
     * gate on it when the thing being drawn *does not exist* below some version, not
     * when it merely behaves differently. Cooldowns is the case that brought it here -
     * nothing has an item cooldown before 1.9, so the readout would be a permanently
     * empty list rather than a wrong one.
     *
     * Like a module's, it gates [running] and deliberately not `enabled`, so the
     * player's own toggle survives: the component pauses on one server and comes back
     * on the next.
     */
    open val inapplicableOnProtocol: String?
        get() = null

    override val running: Boolean
        get() = super.running && inapplicableOnProtocol == null

    override fun onToggled(state: Boolean): Boolean {
        // Say so at the moment of switching it on. A component that is enabled and inert
        // looks exactly like one that is broken - and a HUD element that draws nothing is
        // even harder to tell apart than a module, because there is no arraylist entry to
        // notice its absence in.
        if (state && !AutoConfig.loadingNow) {
            inapplicableOnProtocol?.let { reason ->
                notification(name, reason, NotificationEvent.Severity.ERROR)
            }
        }

        return super<ToggleableValueGroup>.onToggled(state)
    }
    private val defaultAlignment = Alignment(
        alignment.horizontalAlignment,
        alignment.horizontalOffset,
        alignment.verticalAlignment,
        alignment.verticalOffset,
    )
    var zIndex by int("ZIndex", 0, 0..Int.MAX_VALUE).notAnOption()
    val alignment = tree(alignment)

    fun resetAlignment() {
        alignment.setFrom(defaultAlignment)
    }

    protected fun registerComponentListen(valueGroup: ValueGroup) {
        for (v in valueGroup.inner) {
            when (v) {
                is ModeValueGroup<*> -> {
                    v.onChanged {
                        HudComponentManager.updateComponents()
                    }
                    registerComponentListen(v)
                    v.modes.forEach(::registerComponentListen)
                }
                is ValueGroup -> registerComponentListen(v)
                else -> v.onChanged {
                    HudComponentManager.updateComponents()
                }
            }
        }
    }

}
