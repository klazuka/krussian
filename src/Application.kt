package com.klazuka

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.annotations.SerializedName
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.html.Placeholder
import io.ktor.html.Template
import io.ktor.html.insert
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.sessions.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.html.*
import kotlinx.html.ThScope.col
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import kotlin.math.roundToInt

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

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

@KtorExperimentalAPI
@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {}
    }

    val auth0 = object {
        val domain = environment.config.property("krussian.auth0.domain").getString()
        val clientId = environment.config.property("krussian.auth0.clientId").getString()
        val audience = clientId // that's what Auth0 does
        val clientSecret = environment.config.property("krussian.auth0.clientSecret").getString()
        val callbackUrl = environment.config.property("krussian.auth0.callbackUrl").getString()
    }

    install(Authentication) {
        session<UserSession>("SESSION_AUTH") {
            validate { session ->
                // TODO: query the other fields or maybe remove them from the Principal?
                UserPrincipal(
                        name = null,
                        nickname = null,
                        subject = session.subject,
                        pictureUrl = null
                )
            }

            challenge("/login")
        }
    }

    install(Sessions) {
        cookie<UserSession>(
                "KRUSSIAN_SESSION_ID",
                directorySessionStorage(File(".sessions"), cached = true)
        ) {
            cookie.path = "/"
        }
    }

    val airtableClient = AirtableClient(
            apiKey = environment.config.property("krussian.airtable.apiKey").getString(),
            baseId = environment.config.property("krussian.airtable.baseId").getString()
    )

    routing {
        get("/login") {
            val u = URLBuilder("https://klazuka.us.auth0.com/authorize")
            u.parameters.apply {
                append("response_type", "code")
                append("client_id", auth0.clientId)
                append("redirect_uri", auth0.callbackUrl)
                append("scope", "openid profile")
                append("state", "foobar123")
            }
            call.respondRedirect(u.buildString())
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }

        get("/callback") {
            val code = call.request.queryParameters["code"] ?: "?"
            // TODO: verify `state` parameter

            val client = HttpClient(CIO) {
                install(Logging) {
                    level = LogLevel.ALL
                }
                install(JsonFeature) {
                    serializer = GsonSerializer()
                }
            }
            val resp = client.submitForm<AuthResponse>(
                    url = "https://klazuka.us.auth0.com/oauth/token",
                    formParameters = Parameters.build {
                        append("code", code)
                        append("grant_type", "authorization_code")
                        append("client_id", auth0.clientId)
                        append("client_secret", auth0.clientSecret)
                        append("redirect_uri", auth0.callbackUrl)
                    }
            )

            val verifier = JWT.require(Algorithm.HMAC256(auth0.clientSecret))
                    .withAudience(auth0.audience)
                    .withIssuer(auth0.domain)
                    .build()
            val decodedJWT = verifier.verify(resp.idToken)
            val subject = decodedJWT.getClaim("sub").asString()
            if (subject == null) {
                call.respond(Unauthorized, "JWT missing 'sub' claim")
                return@get
            }

            call.sessions.set(UserSession(subject = subject))

            call.respondRedirect("/me")
        }

        authenticate("SESSION_AUTH") {
            get("/me") {
                val user = call.authentication.principal<UserPrincipal>()
                if (user == null) {
                    // TODO: lame
                    call.respond(Unauthorized)
                    return@get
                }
                log.info("Using principal: $user")
                call.respondHtmlTemplate(AppTemplate(user)) {
                    pageTitle { +"Me" }
                    content {
                        h3 { +"Me" }
                        p { +user.subject }
                        p {
                            a(href = "/logout") { +"Logout" }
                        }
                    }
                }
            }
        }

        authenticate("SESSION_AUTH", optional = true) {
            get("/") {
                call.respondHtmlTemplate(AppTemplate(call.principal())) {
                    pageTitle { +"Home" }
                    content {
                        h3 { +"Home" }
                        p { +"дом sweet дом" }
                    }
                }
            }

            get("/decks/all") {
                val decks = airtableClient.getDecks().sortedBy { it.dueDate }
                call.respondHtmlTemplate(AppTemplate(call.principal())) {
                    pageTitle { +"All Decks" }
                    content {
                        h2 { +"Decks" }
                        ul(classes = "nav nav-pills") {
                            li(classes = "nav-item") {
                                a(classes = "nav-link", href = "/decks/due") { +"Due" }
                            }
                            li(classes = "nav-item") {
                                a(classes = "nav-link active", href = "/decks/all") { +"All" }
                            }
                        }
                        table(classes = "table") {
                            thead {
                                tr {
                                    th(scope = col) { +"Name" }
                                    th(scope = col) { +"Due" }
                                    th(scope = col) { +"Repetitions" }
                                }
                            }
                            tbody {
                                for (deck in decks) {
                                    tr {
                                        th {
                                            a(href = deck.url) { +deck.name }
                                        }
                                        td { +deck.dueDate.format(ISO_LOCAL_DATE) }
                                        td { +"${deck.scores.size}" }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            get("/decks/due") {
                val decks = airtableClient.getDecks()
                        .filter { it.dueDate < LocalDate.now().plusDays(1) }
                        .sortedBy { it.dueDate }
                call.respondHtmlTemplate(AppTemplate(call.principal())) {
                    pageTitle { +"Due Decks" }
                    content {
                        h2 { +"Decks" }
                        ul(classes = "nav nav-pills") {
                            li(classes = "nav-item") {
                                a(classes = "nav-link active", href = "/decks/due") { +"Due" }
                            }
                            li(classes = "nav-item") {
                                a(classes = "nav-link", href = "/decks/all") { +"All" }
                            }
                        }
                        for (deck in decks) {
                            div(classes = "card mt-5") {
                                div(classes = "card-body") {
                                    h5(classes = "card-title") {
                                        a(href = deck.url) { +deck.name }
                                    }
                                    h6(classes = "card-subtitle") { +deck.dueDate.format(ISO_LOCAL_DATE) }
                                    p(classes = "card-text") {
                                        when (val s = deck.scores.lastOrNull()) {
                                            null -> +"No scores"
                                            else -> +"Last score: ${(100.0 * s.fields.numCorrect / s.fields.numTotal).roundToInt()}%"
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }

            get("/resources") {
                call.respondHtmlTemplate(AppTemplate(call.principal())) {
                    pageTitle { +"Resources" }
                    content {
                        div(classes = "card") {
                            div(classes = "card-body") {
                                h5(classes = "card-title") { +"Links" }
                                p {
                                    a(href = "https://russianwithmax.com") { +"RussianWithMax" }
                                    +" - podcast for non-native speakers"
                                }
                                p {
                                    a(href = "https://www.orusskomporusski.com") { +"О русском по-русски" }
                                    +" - YouTube channel for Russian grammar, vocab, etc."
                                }
                                p {
                                    a(href = "https://meduza.io") { +"Meduza" }
                                    +" - news, podcasts"
                                }
                                p {
                                    a(href = "https://arzamas.academy") { +"Arzamas" }
                                    +" - art, culture, history, podcasts"
                                }
                            }
                        }
                    }
                }
            }
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }
    }
}

class AppTemplate(val user: UserPrincipal?) : Template<HTML> {
    val content = Placeholder<HtmlBlockTag>()
    val pageTitle = Placeholder<TITLE>()

    override fun HTML.apply() {
        head {
            title {
                +"Krussian - "
                insert(pageTitle)
            }
            link(rel = "stylesheet", href = "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css")
        }
        body {
            div(classes = "container") {
                nav(classes = "navbar navbar-expand navbar-light bg-light mb-3") {
                    a(classes = "navbar-brand", href = "/") { +"Krussian" }
                    div(classes = "navbar-nav mr-auto") {
                        a(classes = "nav-link", href = "/decks/due") { +"Decks" }
                        a(classes = "nav-link", href = "/resources") { +"Resources" }
                        if (user != null)
                            a(classes = "nav-link", href = "/me") { +user.subject }
                        else
                            a(classes = "nav-link", href = "/login") { +"Login" }
                    }
                }
                insert(content)
            }
        }
    }
}
