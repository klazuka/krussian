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
        return client.get<ListDecksResponse>(path = "/v0/$baseId/Decks").records
    }
}

data class ListDecksResponse(
        val records: List<Deck>
)

data class Deck(
        val id: String,
        val fields: DeckFields
)

data class DeckFields(
        @SerializedName("Name")
        val name: String,

        @SerializedName("URL")
        val url: String
)
