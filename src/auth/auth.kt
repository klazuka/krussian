package com.klazuka.krussian.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.annotations.SerializedName
import io.ktor.auth.Principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.config.ApplicationConfig
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.util.KtorExperimentalAPI

interface AuthClient {
    fun makeRedirectUrl(): String
    suspend fun exchangeAuthCodeForSubject(code: String): String
}

@KtorExperimentalAPI
class RealAuthClient(config: ApplicationConfig) : AuthClient {

    private val domain = config.property("krussian.auth0.domain").getString()
    private val clientId = config.property("krussian.auth0.clientId").getString()
    private val audience = config.property("krussian.auth0.clientId").getString() // Yes, same as `clientId`
    private val clientSecret = config.property("krussian.auth0.clientSecret").getString()
    private val callbackUrl = config.property("krussian.auth0.callbackUrl").getString()

    private val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.ALL
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    private val jwtVerifier =
            JWT.require(Algorithm.HMAC256(clientSecret))
                    .withAudience(audience)
                    .withIssuer(domain)
                    .build()

    override fun makeRedirectUrl(): String {
        val u = URLBuilder(domain)
        u.path("authorize")
        u.parameters.apply {
            append("response_type", "code")
            append("client_id", clientId)
            append("redirect_uri", callbackUrl)
            append("scope", "openid profile")
            append("state", "foobar123")
        }
        return u.buildString()
    }

    override suspend fun exchangeAuthCodeForSubject(code: String): String {
        val resp = client.submitForm<AuthResponse>(
                url = "https://klazuka.us.auth0.com/oauth/token",
                formParameters = Parameters.build {
                    append("code", code)
                    append("grant_type", "authorization_code")
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("redirect_uri", callbackUrl)
                }
        )

        return jwtVerifier.verify(resp.idToken)
                .getClaim("sub").asString()
                ?: error("JWT missing 'sub' claim")
    }
}


data class AuthResponse(
        @SerializedName("id_token")
        val idToken: String,

        @SerializedName("access_token")
        val accessToken: String
)

data class UserPrincipal(
        val subject: String,
        val nickname: String?,
        val name: String?,
        val pictureUrl: String?
) : Principal

data class UserSession(
        val subject: String
)