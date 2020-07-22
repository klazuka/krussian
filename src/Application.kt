package com.klazuka

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.html.Placeholder
import io.ktor.html.Template
import io.ktor.html.insert
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.html.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val airtableClient = AirtableClient(
            apiKey = environment.config.property("krussian.airtable.apiKey").getString(),
            baseId = environment.config.property("krussian.airtable.baseId").getString()
    )

    routing {
        get("/") {
            call.respondHtmlTemplate(AppTemplate()) {
                pageTitle { +"Home" }
                content {
                    h3 { +"Home" }
                    p { +"дом sweet дом" }
                }
            }
        }

        get("/decks/all") {
            val decks = airtableClient.getDecks()
            call.respondHtmlTemplate(AppTemplate()) {
                pageTitle { +"All Decks" }
                content {
                    h3 { +"All Decks" }
                    ul {
                        for (deck in decks) {
                            li {
                                a(href = deck.fields.url) { +deck.fields.name }
                            }
                        }
                    }
                }
            }
        }

        get("/decks/due") {
            call.respondHtmlTemplate(AppTemplate()) {
                pageTitle { +"Due Decks" }
                content {
                    h3 { +"Due Decks" }
                    p { +"fire" }
                }
            }
        }

        get("/resources") {
            call.respondHtmlTemplate(AppTemplate()) {
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

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }
    }
}

class AppTemplate : Template<HTML> {
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
                nav(classes = "navbar navbar-expand navbar-light bg-light") {
                    a(classes = "navbar-brand", href = "/") { +"Krussian" }
                    div(classes = "navbar-nav mr-auto") {
                        a(classes = "nav-link", href = "/decks/due") { +"Due" }
                        a(classes = "nav-link", href = "/decks/all") { +"All" }
                        a(classes = "nav-link", href = "/resources") { +"Resources" }
                    }
                }
                insert(content)
            }
        }
    }
}
