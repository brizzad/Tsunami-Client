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

package net.ccbluex.liquidbounce.features.command.commands.client

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig.serializeAutoConfig
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.accountType
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.minecraft.ChatFormatting
import net.minecraft.SharedConstants
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Style
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.EnumSet

/**
 * Debug Command to collect information about the client
 * in order to help developers to fix bugs or help users
 * with their issues.
 *
 * This command will create a JSON file with all the information
 * and write it to a file beside the client config.
 */
object CommandDebug : Command.Factory {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    override fun createCommand() = CommandBuilder.begin("debug")
        .suspendHandler {
            chat("§7Collecting debug information...")

            val buffer = okio.Buffer()

            buffer.outputStream().writer().use {
                serializeAutoConfig(it)
            }
            val autoConfig = buffer.readUtf8()
            val autoConfigFile = writeReport("autoconfig", autoConfig)
            buffer.clear()

            val debugJson = createDebugJson(autoConfigFile.absolutePath)
            buffer.outputStream().writer().use {
                gson.toJson(debugJson, it)
            }
            val reportFile = writeReport("debug", buffer.readUtf8())
            buffer.clear()

            val path = reportFile.absolutePath
            chat(
                "Debug information written to: ".asPlainText(ChatFormatting.GREEN),
                path.asPlainText(Style.EMPTY + ChatFormatting.YELLOW + ClickEvent.CopyToClipboard(path)),
            )
        }
        .build()

    @Suppress("LongMethod")
    private fun createDebugJson(
        autoConfigPaste: String
    ) = JsonObject().apply {
        add("client", JsonObject().apply {
            addProperty("name", LiquidBounce.CLIENT_NAME)
            addProperty("version", LiquidBounce.clientVersion)
            addProperty("commit", LiquidBounce.clientCommit)
            addProperty("branch", LiquidBounce.clientBranch)
            addProperty("development", LiquidBounce.IN_DEVELOPMENT)
            addProperty("usesViaFabricPlus", usesViaFabricPlus)
        })

        add("minecraft", JsonObject().apply {
            addProperty("version", SharedConstants.getCurrentVersion().name())
            addProperty("protocol", SharedConstants.getProtocolVersion())
        })

        add("java", JsonObject().apply {
            addProperty("version", System.getProperty("java.version"))
            addProperty("vendor", System.getProperty("java.vendor"))
        })

        add("os", JsonObject().apply {
            addProperty("name", System.getProperty("os.name"))
            addProperty("version", System.getProperty("os.version"))
            addProperty("architecture", System.getProperty("os.arch"))
        })

        add("user", JsonObject().apply {
            addProperty("language", System.getProperty("user.language"))
            addProperty("country", System.getProperty("user.country"))
            addProperty("timezone", System.getProperty("user.timezone"))
        })

        add("profile", JsonObject().apply {
            addProperty("name", mc.user.name)
            addProperty("uuid", mc.user.profileId.toString())
            addProperty("type", mc.user.accountType)
        })

        add("language", JsonObject().apply {
            addProperty("language", mc.languageManager.selected)
            addProperty("clientLanguage", LanguageManager.clientLanguage.tag)
        })

        add("server", JsonObject().apply {
            mc.currentServer?.let {
                addProperty("name", it.name)
                addProperty("address", it.ip)
                addProperty("protocol", it.protocol)
            }
        })

        addProperty("config", autoConfigPaste)

        add("activeModules", JsonArray().apply {
            ModuleManager.filter { it.running }.forEach { module ->
                add(JsonPrimitive(module.name))
            }
        })

        add("scripts", JsonArray().apply {
            ScriptManager.scripts.forEach { script ->
                add(JsonObject().apply {
                    addProperty("name", script.scriptName)
                    addProperty("version", script.scriptVersion)
                    addProperty("author", script.scriptAuthors.joinToString(", "))
                    addProperty("path", script.file.path)
                })
            }
        })

        add("enemies", publicGson.toJsonTree(GlobalSettingsTarget.combat, EnumSet::class.javaObjectType))
    }

    /**
     * Writes the report to a file beside the client config and returns its path.
     *
     * Upstream POSTed this to paste.ccbluex.net, which put the user's config,
     * loaded scripts and target settings on a third-party host under a public
     * URL. Keeping it on disk lets the user decide whether to share it, and
     * with whom.
     */
    private fun writeReport(name: String, content: String): File {
        val folder = File(ConfigSystem.rootFolder, "debug").apply { mkdirs() }
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        return File(folder, "$name-$stamp.txt").apply { writeText(content) }
    }

}
