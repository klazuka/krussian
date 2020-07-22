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
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

class AirtableClient(
        val apiKey: String,
        val baseId: String
) {
    private val client = HttpClient(CIO) {
        defaultRequest {
            host = "api.airtable.com"
            header("Authorization", "Bearer $apiKey")
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    suspend fun getDecks(): List<Deck> {
        val rawDecks = client.get<ListDecksResponse>(path = "/v0/$baseId/Decks").records
        val scores = client.get<ListScoresResponse>(path = "/v0/$baseId/Scores").records
        return resolveDecks(rawDecks, scores)
    }

    private fun resolveDecks(decks: List<RawDeck>, scores: List<Score>): List<Deck> =
            decks.map { deck ->
                val actualScores = resolveScores(scores, deck.fields.scoreRefs)
                Deck(
                        id = deck.id,
                        name = deck.fields.name,
                        url = deck.fields.url,
                        scores = actualScores,
                        dueDate = nextDueDate(actualScores)
                )
            }

    private fun nextDueDate(scores: List<Score>): String {
        if (scores.isEmpty()) return LocalDate.now().format(ISO_LOCAL_DATE)
        val mostRecentScore = scores.last()
        val numDaysOffset = fibonacci(scores.size)
        return LocalDate.parse(mostRecentScore.fields.date, ISO_LOCAL_DATE)
                .plusDays(numDaysOffset)
                .format(ISO_LOCAL_DATE)
    }

    private fun fibonacci(n: Int): Long =
            when (n) {
                0 -> 1
                1 -> 1
                else -> fibonacci(n - 2) + fibonacci(n - 1)
            }

    private fun resolveScores(allScores: List<Score>, refs: List<String>): List<Score> =
            // TODO sort the result by date from oldest to newest
            allScores.filter { it.id in refs }
}

data class Deck(
        val id: String,
        val name: String,
        val url: String,
        val scores: List<Score>,
        val dueDate: String
)

data class ListDecksResponse(
        val records: List<RawDeck>
)

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
        val records: List<Score>
)

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