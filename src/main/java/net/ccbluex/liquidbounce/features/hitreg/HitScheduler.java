/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
 *
 * Portions derived from BetterHitreg (https://github.com/jasspir/BetterHitreg)
 * Copyright (c) Jass, licensed under the Apache License, Version 2.0.
 * See package-info.java for what was taken and what was left behind.
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
package net.ccbluex.liquidbounce.features.hitreg;

import net.minecraft.client.Minecraft;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs hit feedback on the render thread, optionally after a delay.
 *
 * The delay exists because zero is not always what a player wants: drawing the
 * hit the instant the mouse goes down can feel ahead of the swing animation
 * itself. Anything above zero needs a timer, and the timer thread only ever
 * hands work back to the client rather than touching the game from off-thread.
 *
 * The executor is created on first use, so a player leaving the delay at zero
 * never pays for a thread.
 */
final class HitScheduler {

    private HitScheduler() {
    }

    private static volatile ScheduledExecutorService timer;

    static void schedule(int delayMs, Runnable task) {
        Minecraft client = Minecraft.getInstance();

        if (delayMs <= 0) {
            client.execute(task);
            return;
        }

        timer().schedule(() -> client.execute(task), delayMs, TimeUnit.MILLISECONDS);
    }

    private static ScheduledExecutorService timer() {
        ScheduledExecutorService existing = timer;
        if (existing != null) {
            return existing;
        }

        synchronized (HitScheduler.class) {
            if (timer == null) {
                timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "Tsunami Hitreg");
                    // Daemon, so a pending delay cannot hold the game open on quit.
                    thread.setDaemon(true);
                    return thread;
                });
            }
            return timer;
        }
    }
}
