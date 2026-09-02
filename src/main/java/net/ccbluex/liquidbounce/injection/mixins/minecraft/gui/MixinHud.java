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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.OverlayMessageEvent;
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent;
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.ModuleCrosshair;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponent;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.armorhud.ArmorHudRenderer;
import net.ccbluex.liquidbounce.integration.theme.component.components.ArmorHudComponent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModulePotionTimers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.ccbluex.liquidbounce.features.effectbars.EffectBarRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Hud.class)
public abstract class MixinHud {


    @Shadow
    @Final
    private static net.minecraft.resources.Identifier HOTBAR_SPRITE;

    @Shadow
    @Final
    private static net.minecraft.resources.Identifier HOTBAR_OFFHAND_LEFT_SPRITE;

    @Final
    @Shadow
    private static Identifier POWDER_SNOW_OUTLINE_LOCATION;

    @Shadow
    @Nullable
    protected abstract Player getCameraPlayer();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract void extractSlot(GuiGraphicsExtractor context, int x, int y, DeltaTracker tickCounter, Player player, ItemStack stack, int seed);

    /**
     * Hook render hud event at the top layer
     */
    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
    private void hookRenderEventStart(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        EventManager.INSTANCE.callEvent(new OverlayRenderEvent(context, tickCounter.getGameTimeDeltaPartialTick(false)));

        // Draw after overlay event
        var component = HudComponentManager.getComponentWithTweak(HudComponentTweak.TWEAK_HOTBAR);
        if (component != null && component.getRunning() &&
                minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            extractHotbarForHud(context, tickCounter, component);
        }
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderSpyglassOverlay(GuiGraphicsExtractor context, float scale, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.SPYGLASS_OVERLAY)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(GuiGraphicsExtractor context, Identifier texture, float opacity, CallbackInfo callback) {
        if (!ModuleAntiBlind.INSTANCE.getRunning()) {
            return;
        }

        if (!ModuleAntiBlind.canRender(DoRender.PUMPKIN_BLUR) && ModuleAntiBlind.TEXTURE_PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
            return;
        }

        if (!ModuleAntiBlind.canRender(DoRender.POWDER_SNOW_FOG) && POWDER_SNOW_OUTLINE_LOCATION.equals(texture)) {
            callback.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void hookFreeCamRenderCrosshairInThirdPerson(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_CROSSHAIR)
                || ModuleCrosshair.INSTANCE.getEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderPortalOverlay(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.PORTAL_OVERLAY)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void renderScoreboardSidebar(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_SCOREBOARD)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void hookRenderHotbar(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.TWEAK_HOTBAR)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void hookRenderStatusBars(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_STATUS_BAR)) {
            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "nextContextualInfoState", at = @At("RETURN"))
    private Hud.ContextualInfo tweakExpBar(Hud.ContextualInfo original) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_EXP_BAR) && original == Hud.ContextualInfo.EXPERIENCE) {
            return Hud.ContextualInfo.EMPTY;
        }
        return original;
    }

    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z"))
    private boolean tweakExpLevelText(MultiPlayerGameMode instance, Operation<Boolean> original) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_EXP_BAR)) {
            return false;
        }
        return original.call(instance);
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void hookRenderHeldItemTooltip(CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_HELD_ITEM_TOOL_TIP)) {
            ci.cancel();
        }
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void hookSetOverlayMessage(Component message, boolean tinted, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new OverlayMessageEvent(message, tinted));

        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_OVERLAY_MESSAGE)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void hookRenderStatusEffectOverlay(CallbackInfo ci) {
        // PotionTimers decorates these very icons, so it has to win over the
        // theme tweak that hides them. Without this the module loads, enables,
        // and draws nothing - the whole method is cancelled before it reaches
        // the injection the bars are drawn from.
        if (ModulePotionTimers.INSTANCE.getRunning()) {
            return;
        }

        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_STATUS_EFFECT_OVERLAY)) {
            ci.cancel();
        }
    }

    /**
     * Draws the depletion bar under each status effect icon.
     *
     * Injected where vanilla looks up the sprite for an icon, because that
     * is the one point where the effect and its screen position are both in
     * scope. Drawing afterwards would mean recomputing the layout and
     * getting it subtly wrong the moment vanilla changes it.
     */
    @Inject(
        method = "extractEffects",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;getMobEffectSprite(Lnet/minecraft/core/Holder;)Lnet/minecraft/resources/Identifier;",
            ordinal = 0
        )
    )
    private void hookPotionTimerBars(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci,
                                     @Local(name = "instance") MobEffectInstance instance,
                                     @Local(name = "x") int x,
                                     @Local(name = "y") int y) {
        EffectBarRenderer.render(context, tickCounter, instance, x, y, EffectBarRenderer.ICON_SIZE);
    }


    @Unique
    private void extractHotbarForHud(GuiGraphicsExtractor context, DeltaTracker tickCounter, HudComponent hudComponent) {
        var playerEntity = this.getCameraPlayer();
        if (playerEntity == null) {
            return;
        }

        // All values are measured, not calculated (with scale 2)
        // TODO: fix scaled positions
        final float guiScale = this.minecraft.getWindow().getGuiScale();

        float slotWidth = 22.5F;
        int offset = 98;
        var bounds = hudComponent.getAlignment().getBounds(203f, 25f);

        int xCenter = (int) bounds.xCenter();
        float y = bounds.yMin() + 5f;

        int seed = 1;
        List<ItemStack> items = playerEntity.getInventory().getNonEquipmentItems();
        for (int m = 0; m < Inventory.SELECTION_SIZE; ++m) {
            float x = xCenter - offset + m * slotWidth;
            this.extractSlot(context, (int) x, (int) y, tickCounter, playerEntity, items.get(m), seed++);
        }

        ItemStack offHandStack = playerEntity.getOffhandItem();
        if (!offHandStack.isEmpty()) {
            this.extractSlot(context, xCenter - offset - 32, (int) y, tickCounter, playerEntity, offHandStack, seed);
        }
    }

    @ModifyExpressionValue(method = "extractCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnCrosshair(CameraType original) {
        return PerspectiveEvent.INSTANCE.getPerspective();
    }

    @ModifyExpressionValue(method = "extractCameraOverlays",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnMiscOverlays(CameraType original) {
        return PerspectiveEvent.INSTANCE.getPerspective();
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void hookRenderTitleAndSubtitle(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.TITLE)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void hookNauseaOverlay(GuiGraphicsExtractor context, float distortionStrength, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.NAUSEA)) {
            ci.cancel();
        }
    }



    /**
     * Draws the armour widget once the hotbar is done.
     *
     * TAIL of the hotbar extraction rather than a separate render pass, so
     * the widget is layered with the hotbar it is anchored to and inherits
     * the same scaling.
     */
    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    private void hookArmorHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, CallbackInfo ci) {
        var module = ArmorHudComponent.INSTANCE;
        if (!module.getRunning()) {
            return;
        }

        var player = ArmorHudRenderer.cameraPlayer();
        if (player == null) {
            return;
        }

        var rect = ArmorHudRenderer.widgetRect(graphics, player);
        if (rect == null) {
            return;
        }

        var items = ArmorHudRenderer.armorItems(player);
        int size = ArmorHudRenderer.SIZE;
        int step = ArmorHudRenderer.STEP;
        int textureWidth = size + ((items.size() - 1) * step);
        boolean vertical = module.getOrientation() == ArmorHudComponent.Orientation.VERTICAL;

        // The background is drawn in the widget's own space; rotating for the
        // vertical layout is cheaper than a second set of blit coordinates.
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.getX(), rect.getY());
        if (vertical) {
            graphics.pose().rotate(net.minecraft.util.Mth.HALF_PI).translate(0, -size);
        }

        int white = ARGB.white(1.0f);
        switch (module.getStyle()) {
            case HOTBAR -> {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22,
                    0, 0, 0, 0, textureWidth - 3, size, white);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22,
                    182 - 3, 0, textureWidth - 3, 0, 3, size, white);
            }
            case ROUNDED -> {
                if (items.size() > 1) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24,
                        0, 1, 0, 0, 3, size, white);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22,
                        3, 0, 3, 0, textureWidth - 6, size, white);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24,
                        size - 3, 1, textureWidth - 3, 0, 3, size, white);
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24,
                        0, 1, 0, 0, size, size, white);
                }
            }
            case NONE -> { }
        }
        graphics.pose().popMatrix();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int x = rect.getX() + (vertical ? 0 : step * i);
            int y = rect.getY() + (vertical ? step * i : 0);

            if (module.getWarnOnLowDurability() && ArmorHudRenderer.isLowDurability(stack)) {
                graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1,
                    module.getWarningColor().argb());
            }

            this.extractSlot(graphics, x + 3, y + 3, tickCounter, player, stack, i + 1);

            String durability = ArmorHudRenderer.durabilityText(stack);
            if (durability != null) {
                int textY = module.getAnchor().isTop() ? y + size : y - net.minecraft.client.Minecraft.getInstance().font.lineHeight;
                graphics.centeredText(net.minecraft.client.Minecraft.getInstance().font, durability, x + (size / 2), textY,
                    ARGB.opaque(stack.getBarColor()));
            }
        }
    }
}
