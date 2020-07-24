package com.klazuka

import com.google.gson.annotations.SerializedName
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.host
import io.ktor.config.ApplicationConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

interface AirtableClient {
    suspend fun getDecks(): List<Deck>
}

class RealAirtableClient(
        val apiKey: String,
        val baseId: String
) : AirtableClient {

    constructor(config: ApplicationConfig) : this(
            apiKey = config.property("krussian.airtable.apiKey").getString(),
            baseId = config.property("krussian.airtable.baseId").getString()
    )

    private val client = HttpClient(CIO) {
        defaultRequest {
            host = "api.airtable.com"
            header("Authorization", "Bearer $apiKey")
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    override suspend fun getDecks(): List<Deck> {
        val rawDecks: List<RawDeck> = getAll("Decks") { client.get<ListDecksResponse>(it) }
        val scores: List<Score> = getAll("Scores") { client.get<ListScoresResponse>(it) }
        return resolveDecks(rawDecks, scores)
    }

    /// Fetches all pages from the API.
    private inline fun <T> getAll(table: String, fetch: (String) -> AirtableResponse<T>): List<T> {
        // NOTE: I couldn't figure out how to handle the pagination using Kotlin generics + Gson.
        //       The crux of the problem is that if you use an interface or an abstract class to
        //       model the paginated response envelope, then Gson cannot construct it when it does
        //       it's deserialization magic. So instead I just made it the caller's responsibility
        //       to provide a concrete response type. There's probably a smarter way to do this.
        val acc = mutableListOf<T>()
        var offset: String? = null
        do {
            var path = "/v0/$baseId/$table"
            if (offset != null)
                path += "?offset=$offset"
            val resp = fetch(path)
            acc += resp.records
            offset = resp.offset
        } while (offset != null)
        return acc
    }

    private fun resolveDecks(decks: List<RawDeck>, scores: List<Score>): List<Deck> =
            decks.map { deck ->
                val actualScores = resolveScoreRefs(scores, deck.fields.scoreRefs)
                Deck(
                        id = deck.id,
                        name = deck.fields.name,
                        url = deck.fields.url,
                        scores = actualScores,
                        dueDate = nextDueDate(actualScores)
                )
            }

    private fun nextDueDate(scores: List<Score>): LocalDate =
            when {
                scores.isEmpty() -> LocalDate.now()
                else -> LocalDate.parse(scores.last().fields.date, ISO_LOCAL_DATE)
                        .plusDays(fibonacci(scores.size))
            }

    private fun fibonacci(n: Int): Long =
            when (n) {
                0 -> 1
                1 -> 1
                else -> fibonacci(n - 2) + fibonacci(n - 1)
            }

    private fun resolveScoreRefs(allScores: List<Score>, refs: List<String>): List<Score> =
            allScores.filter { it.id in refs }
                    .sortedBy { it.fields.date }
}

data class Deck(
        val id: String,
        val name: String,
        val url: String,
        val scores: List<Score>,
        val dueDate: LocalDate
)

abstract class AirtableResponse<T> {
    abstract val records: List<T>
    abstract val offset: String?
}

data class ListDecksResponse(
        override val records: List<RawDeck>,
        override val offset: String?
) : AirtableResponse<RawDeck>()

data class RawDeck(
        val id: String,
        val fields: DeckFields
)

data class DeckFields(
        @SerializedName("Name")
        val name: String,

        @SerializedName("URL")
        val url: String,

        @SerializedName("Scores")
        val scoreRefs: List<String>
)

data class ListScoresResponse(
        override val records: List<Score>,
        override val offset: String?
) : AirtableResponse<Score>()

data class Score(
        val id: String,
        val fields: ScoreFields
)

data class ScoreFields(
        @SerializedName("Num Correct")
        val numCorrect: Int,

        @SerializedName("Num Total")
        val numTotal: Int,

        @SerializedName("Date")
        val date: String
)