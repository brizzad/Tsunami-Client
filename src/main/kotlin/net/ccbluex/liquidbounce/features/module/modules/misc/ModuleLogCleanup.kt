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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * LogCleanup
 *
 * Deletes archived game logs older than a set age.
 *
 * The `logs` folder is append-only as far as Minecraft is concerned: every
 * launch leaves a gzipped copy behind and nothing ever removes one. A client
 * that has been in use for a year carries several hundred megabytes of them,
 * which is the sort of thing nobody notices until a disk is full.
 *
 * ## What it will and will not delete
 *
 * Only the rotated archives in `logs` - the gzipped `.log.gz` files. The live `latest.log` and
 * `debug.log` are never touched, because those are what anybody diagnosing a
 * crash actually reads. Deletion is by modification time, and it runs when the
 * module is switched on rather than on a timer, so it is an action the player
 * takes rather than something happening behind them.
 *
 * A deletion this module makes is reported in chat with a count. Silent file
 * removal is not something a client should do.
 */
object ModuleLogCleanup : ClientModule("LogCleanup", ModuleCategories.MISC) {

    /** Archives older than this many days go. */
    private val keepDays by int("KeepDays", 14, 1..365)

    /**
     * Report what would go without deleting anything. On by default: the first
     * run of a thing that deletes files should show its working.
     */
    private val dryRun by boolean("DryRun", true)

    /** Only these are ever considered. `latest.log` is deliberately not here. */
    private const val ARCHIVE_SUFFIX = ".log.gz"

    override suspend fun enabledEffect() {
        val logs = mc.gameDirectory.toPath().resolve("logs")
        val cutoff = Instant.now().minus(Duration.ofDays(keepDays.toLong()))

        val stale = try {
            collectStale(logs, cutoff)
        } catch (e: IOException) {
            logger.warn("LogCleanup could not read $logs", e)
            chat("Could not read the logs folder: ${e.message}", this)
            return
        }

        if (!dryRun) {
            stale.forEach(::delete)
        }

        chat(report(stale), this)
    }

    /**
     * Every archive older than the cutoff.
     *
     * Split out from the caller so the decision of what to delete is one
     * readable filter chain rather than a loop of early exits interleaved with
     * the deleting and the reporting.
     */
    private fun collectStale(logs: Path, cutoff: Instant): List<Path> =
        Files.newDirectoryStream(logs).use { entries ->
            entries.filter { entry ->
                entry.isRegularFile() &&
                    entry.name.endsWith(ARCHIVE_SUFFIX) &&
                    !Files.getLastModifiedTime(entry).toInstant().isAfter(cutoff)
            }
        }

    private fun report(stale: List<Path>): String {
        if (stale.isEmpty()) {
            return "No archived logs older than $keepDays days."
        }

        val bytes = stale.sumOf { runCatching { Files.size(it) }.getOrDefault(0L) }
        val megabytes = bytes / 1024.0 / 1024.0
        val plural = if (stale.size == 1) "" else "s"
        val summary = "%d archived log%s, %.1f MB".format(stale.size, plural, megabytes)

        return if (dryRun) {
            "Would delete $summary. Turn DryRun off to actually remove them."
        } else {
            "Deleted $summary."
        }
    }

    private fun delete(path: Path) {
        try {
            Files.deleteIfExists(path)
        } catch (e: IOException) {
            logger.warn("LogCleanup could not delete $path", e)
        }
    }

}
