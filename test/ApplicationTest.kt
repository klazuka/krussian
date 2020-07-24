package com.klazuka

import com.nhaarman.mockitokotlin2.mock
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
    val airtableClient: AirtableClient = mock()
    val authClient: AuthClient = mock()

    @Test
    fun testRoot() {
        withTestApplication({
            moduleWithDeps(true, airtableClient, authClient)
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
            moduleWithDeps(true, airtableClient, authClient)
        }) {
            handleRequest(HttpMethod.Get, "/me").apply {
                assertEquals(HttpStatusCode.Found, response.status())
                assertEquals("/login", response.headers["Location"])
            }
        }
    }
}
