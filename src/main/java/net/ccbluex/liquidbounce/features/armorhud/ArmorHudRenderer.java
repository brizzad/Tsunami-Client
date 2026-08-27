/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
 *
 * Derived from uku's Armor HUD (https://github.com/uku3lig/armor-hud)
 * Copyright (c) uku3lig and contributors, licensed under the MIT License.
 * MIT permits inclusion in a GPL-3.0 work; this file is distributed under the
 * GPL as part of Tsunami, and the MIT notice above is retained as that licence
 * requires.
 *
 * Taken: the widget layout maths, the offhand and attack-indicator spacing
 * rules, the slot-selection modes and the low-durability rule. Left behind:
 * upstream config library, its bobbing warning sprite and armour-breaking
 * sound (both need bundled assets), and the layout permutations Tsunami does
 * not expose.
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
package net.ccbluex.liquidbounce.features.armorhud;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleArmorHud;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the armour widget goes and what belongs in it.
 *
 * The layout is fiddlier than it looks, and that is the reason this was taken
 * from a mature mod rather than written: the widget has to dodge the offhand
 * slot, the attack indicator, and the fact that both of those move to the other
 * side when the player is left-handed. Getting it slightly wrong means the
 * widget sits on top of the hotbar for half the people using it.
 */
public final class ArmorHudRenderer {

    private ArmorHudRenderer() {
    }

    /** One slot, including its border. */
    public static final int SIZE = 22;

    /** Distance between the left edges of two adjacent slots. */
    public static final int STEP = 20;

    /** Half the hotbar, measured from the centre of the screen. */
    private static final int HOTBAR_OFFSET = 98;

    /** Width of the offhand slot beside the hotbar. */
    private static final int OFFHAND_OFFSET = 29;

    /** Width of the attack indicator when it sits beside the hotbar. */
    private static final int ATTACK_INDICATOR_OFFSET = 23;

    /** Vanilla equipment slots, helmet first. */
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
    };

    public static @Nullable Player cameraPlayer() {
        return Minecraft.getInstance().getCameraEntity() instanceof Player player ? player : null;
    }

    /**
     * The armour to draw, in display order.
     *
     * Returns an empty list when the widget should not appear at all, which is
     * how the display modes are enforced in one place rather than at every
     * drawing site.
     */
    public static List<ItemStack> armorItems(Player player) {
        ModuleArmorHud module = ModuleArmorHud.INSTANCE;

        List<ItemStack> all = new ArrayList<>(SLOTS.length);
        for (EquipmentSlot slot : SLOTS) {
            all.add(player.getItemBySlot(slot));
        }

        List<ItemStack> shown = switch (module.getWidgetShown()) {
            case ALWAYS -> all;
            case IF_ANY_PRESENT -> all.stream().allMatch(ItemStack::isEmpty) ? List.of() : all;
            case NOT_EMPTY -> all.stream().filter(stack -> !stack.isEmpty()).toList();
            case DAMAGED_PIECES -> all.stream().filter(ArmorHudRenderer::isLowDurability).toList();
        };

        if (module.getReversed()) {
            shown = new ArrayList<>(shown);
            java.util.Collections.reverse(shown);
        }

        return shown;
    }

    /**
     * The widget's bounding box, or null when there is nothing to draw.
     *
     * The two multipliers are upstream's way of folding four anchor cases and
     * two sides into one expression. `sideMultiplier` flips the direction the
     * widget grows in; `sideOffsetMultiplier` shifts it back by its own width
     * when it grows leftwards.
     */
    public static @Nullable Rect2i widgetRect(GuiGraphicsExtractor graphics, Player player) {
        ModuleArmorHud module = ModuleArmorHud.INSTANCE;
        List<ItemStack> items = armorItems(player);

        if (items.isEmpty()) {
            return null;
        }

        var anchor = module.getAnchor();
        var side = module.getSide();
        boolean hotbarAnchored = anchor == ModuleArmorHud.Anchor.HOTBAR;
        boolean growsLeft = (hotbarAnchored && side == ModuleArmorHud.Side.LEFT)
                || (!hotbarAnchored && side == ModuleArmorHud.Side.RIGHT);

        int sideMultiplier = growsLeft ? -1 : 1;
        int sideOffsetMultiplier = growsLeft ? -1 : 0;

        int addedHotbarOffset = switch (module.getOffhandBehavior()) {
            case ALWAYS_IGNORE -> 0;
            case ALWAYS_LEAVE_SPACE ->
                player.getMainArm() == mainArmFor(side) ? ATTACK_INDICATOR_OFFSET : OFFHAND_OFFSET;
            case ADHERE -> adhereOffset(player, side);
        };

        int textureWidth = SIZE + ((items.size() - 1) * STEP);
        boolean vertical = module.getOrientation() == ModuleArmorHud.Orientation.VERTICAL;
        int widgetWidth = vertical ? SIZE : textureWidth;
        int widgetHeight = vertical ? textureWidth : SIZE;

        int x = module.getOffsetX() * sideMultiplier + switch (anchor) {
            case TOP_CENTER -> (graphics.guiWidth() - widgetWidth) / 2;
            case TOP, BOTTOM -> (widgetWidth - graphics.guiWidth()) * sideOffsetMultiplier;
            case HOTBAR -> graphics.guiWidth() / 2
                    + ((HOTBAR_OFFSET + addedHotbarOffset) * sideMultiplier)
                    + (widgetWidth * sideOffsetMultiplier);
        };

        int y = switch (anchor) {
            case BOTTOM, HOTBAR -> graphics.guiHeight() - widgetHeight - module.getOffsetY();
            case TOP, TOP_CENTER -> module.getOffsetY();
        };

        return new Rect2i(x, y, widgetWidth, widgetHeight);
    }

    /**
     * Space to leave only when something is actually occupying it.
     *
     * On the main-hand side that is the attack indicator, and only while the
     * cooldown is running; on the other side it is the offhand slot, and only
     * while something is held there.
     */
    private static int adhereOffset(Player player, ModuleArmorHud.Side side) {
        if (player.getMainArm() == mainArmFor(side)) {
            boolean hotbarIndicator = Minecraft.getInstance().options.attackIndicator().get()
                    == AttackIndicatorStatus.HOTBAR;
            if (hotbarIndicator && player.getAttackStrengthScale(0) < 1) {
                return ATTACK_INDICATOR_OFFSET;
            }
            return 0;
        }

        return player.getOffhandItem().isEmpty() ? 0 : OFFHAND_OFFSET;
    }

    private static net.minecraft.world.entity.HumanoidArm mainArmFor(ModuleArmorHud.Side side) {
        return side == ModuleArmorHud.Side.LEFT
                ? net.minecraft.world.entity.HumanoidArm.LEFT
                : net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    /**
     * Whether a piece is close enough to breaking to be worth flagging.
     *
     * Two thresholds rather than one, because a percentage alone is wrong at
     * both ends: 10% of leather is three hits, and 10% of netherite is still a
     * long fight.
     */
    public static boolean isLowDurability(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return false;
        }

        ModuleArmorHud module = ModuleArmorHud.INSTANCE;
        int damage = stack.getDamageValue();
        int maxDamage = stack.getMaxDamage();
        int remaining = maxDamage - damage;
        double fraction = 1.0 - ((double) damage / maxDamage);

        return fraction <= module.getWarnBelowPercent() / 100.0
                || remaining <= module.getWarnBelowDurability();
    }

    /** Durability text for a slot, or null when nothing should be drawn. */
    public static @Nullable String durabilityText(ItemStack stack) {
        ModuleArmorHud module = ModuleArmorHud.INSTANCE;

        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return null;
        }

        return switch (module.getDurabilityDisplay()) {
            case BAR -> null;
            case NUMERIC -> String.valueOf(stack.getMaxDamage() - stack.getDamageValue());
            case PERCENTAGE -> {
                if (stack.getDamageValue() == 0) {
                    yield null;
                }
                double fraction = 1.0 - (double) stack.getDamageValue() / stack.getMaxDamage();
                yield ((int) Math.floor(fraction * 100)) + "%";
            }
        };
    }
}
