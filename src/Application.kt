package com.klazuka

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    routing {
        get("/") {
            call.respondHtml {
                body {
                    h1 { +"Home" }
                    p { a(href = "/decks/all") { +"All Decks" } }
                    p { a(href = "/decks/due") { +"Due Decks" } }
                    p { a(href = "/resources") { +"Resources" } }
                }
            }

        }

        get("/decks/all") {
            call.respondHtml {
                body {
                    h1 { +"All Decks" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }

        get("/decks/due") {
            call.respondHtml {
                body {
                    h1 { +"Due Decks" }
                    p { +"fire" }
                }
            }
        }

        get("/resources") {
            call.respondHtml {
                head {
                    link(rel = "stylesheet", href = "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css")
                }
                body {
                    div(classes = "container") {
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

