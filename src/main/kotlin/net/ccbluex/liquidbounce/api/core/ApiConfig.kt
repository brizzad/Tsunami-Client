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
package net.ccbluex.liquidbounce.api.core

import net.ccbluex.liquidbounce.utils.client.logger
import org.apache.commons.lang3.RandomStringUtils

class ApiConfig(
    val url: String,
    @Suppress("unused")
    val secure: Boolean = url.startsWith("https://"),
    val sessionToken: String
) {

    val apiEndpointV1 = "$url/api/v1"
    val apiEndpointV3 = "$url/api/v3"

    companion object {

        /**
         * API hosts, tried in order.
         *
         * Deliberately unreachable. Tsunami has no backend, and the upstream
         * entries here pointed at CCBlueX production: left in place the client
         * silently talks to their servers on every start. RFC 2606 reserves
         * .invalid so this can never resolve and fails closed.
         *
         * Replace with the real host once one exists. Note the launcher can
         * override this at runtime via the
         * net.ccbluex.liquidbounce.api.url system property.
         */
        private val API_URLS = arrayOf(
            "https://api.tsunami.invalid",

            // Non-secure connection requires additional confirmation from the user,
            // as they are vulnerable to MITM attacks and data leaks.
            // A VPN or a proxy can be used to secure the connection.
            // TODO: Because we cannot request confirmation from the user, we should not use this URL at all.
            //   The only way to use this URL is to set the property `net.ccbluex.liquidbounce.api.url` to
            //   this URL which the LiquidLauncher does when the user has confirmed it.
            // "http://nossl.api.liquidbounce.net"
        )

        /** Asset CDN. Unreachable for the same reason as [API_URLS]. */
        const val CLIENT_CDN = "https://cdn.tsunami.invalid"

        /**
         * Client-account OAuth. Unreachable, and the client id below is a
         * placeholder: the upstream value identified CCBlueX's own OAuth
         * application, which is not ours to authenticate against.
         *
         * The launcher-side equivalent of this whole layer was deleted with
         * the premium tier; the client half is still wired up and wants the
         * same treatment.
         */
        const val AUTH_BASE_URL = "https://auth.tsunami.invalid/application/o"
        const val AUTH_AUTHORIZE_URL = "$AUTH_BASE_URL/authorize/"
        const val AUTH_CLIENT_ID = "TSUNAMI_OAUTH_CLIENT_ID_PLACEHOLDER"

        /**
         * This makes sense because we want forks to be able to use this API and not only the official client.
         * It also allows us to use API endpoints for legacy on other branches.
         */
        const val API_BRANCH = "nextgen"

        /**
         * Player head renders.
         *
         * This is a data flow, not just a dead link: upstream sent the player
         * UUID or username to CCBlueX every time a head was drawn. The same
         * leak was fixed in the launcher by rendering the head locally from
         * Mojang's own profile data, and this should follow. Until then it
         * points nowhere and heads fall back to the default skin.
         */
        private const val AVATAR_BASE_URL = "https://avatar.tsunami.invalid/avatar"
        const val AVATAR_UUID_URL = "$AVATAR_BASE_URL/%s/100"
        const val AVATAR_USERNAME_URL = "$AVATAR_BASE_URL/%s"

        /**
         * Defines the API environment for LiquidBounce.
         * This should be initialized before the API is used through [net.ccbluex.liquidbounce.LiquidBounce].
         */
        val config by AsyncLazy<ApiConfig> {
            lookup()
        }

        suspend fun lookup(): ApiConfig {
            val sessionToken = System.getProperty(
                "net.ccbluex.liquidbounce.api.token",
                RandomStringUtils.secure().nextAlphanumeric(16)
            )
            logger.info("API Session Token: $sessionToken")

            // We trust LiquidLauncher to have found the correct API URL
            val propertyUrl = System.getProperty("net.ccbluex.liquidbounce.api.url")
            if (propertyUrl != null) {
                logger.info("Using API URL from system property: $propertyUrl")
                return ApiConfig(propertyUrl, sessionToken = sessionToken)
            }

            // Try API urls until we find one that works
            logger.info("Looking up available API endpoints...")
            for (url in API_URLS) {
                try {
                    // Throws [HttpException] when not successful
                    HttpClient.request(url, HttpMethod.HEAD).close()

                    logger.info("API endpoint '$url' is available")
                    return ApiConfig(
                        url,
                        sessionToken = sessionToken
                    )
                } catch (e: HttpException) {
                    logger.debug("API endpoint '$url' returned status code: ${e.code}")
                } catch (e: Exception) {
                    logger.error("Failed to connect to API endpoint '$url'", e)
                }
            }

            logger.error("No API endpoints are available, using default: ${API_URLS[0]}")
            return ApiConfig(
                API_URLS[0],
                sessionToken = sessionToken
            )
        }

    }





}
