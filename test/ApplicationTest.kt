package com.klazuka

import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@KtorExperimentalAPI
class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({
            (environment.config as MapApplicationConfig).apply {
                put("krussian.auth0.domain", "auth0_domain")
                put("krussian.auth0.clientId", "auth0_client_id")
                put("krussian.auth0.clientSecret", "auth0_client_secret")
                put("krussian.auth0.callbackUrl", "auth0_callback_url")
                put("krussian.airtable.baseId", "airtable_base_id")
                put("krussian.airtable.apiKey", "airtable_api_key")
            }
            module(testing = true)
        }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue { "text/html" in response.contentType().toString() }
            }
        }
    }

    @Test
    fun `profile page requires auth and redirects to login page`() {
        withTestApplication({
            (environment.config as MapApplicationConfig).apply {
                put("krussian.auth0.domain", "auth0_domain")
                put("krussian.auth0.clientId", "auth0_client_id")
                put("krussian.auth0.clientSecret", "auth0_client_secret")
                put("krussian.auth0.callbackUrl", "auth0_callback_url")
                put("krussian.airtable.baseId", "airtable_base_id")
                put("krussian.airtable.apiKey", "airtable_api_key")
            }
            module(testing = true)
        }) {
            handleRequest(HttpMethod.Get, "/me").apply {
                assertEquals(HttpStatusCode.Found, response.status())
                assertEquals("/login", response.headers["Location"])
            }
        }
    }
}
