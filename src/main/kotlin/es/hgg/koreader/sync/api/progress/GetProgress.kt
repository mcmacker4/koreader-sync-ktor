package es.hgg.koreader.sync.api.progress

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import es.hgg.koreader.sync.Documents
import es.hgg.koreader.sync.api.sendErrorInternal
import es.hgg.koreader.sync.loggedUser
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@JsonPropertyOrder("device_id", "progress", "document", "percentage", "timestamp", "device")
data class Output(
    val percentage: Float,
    @get:JsonSerialize(using = ProgressSerializer::class)
    val progress: String,
    val device: String,
    @get:JsonProperty("device_id")
    val deviceId: String?,
    val timestamp: Long,
    val document: String,
)

class ProgressSerializer : JsonSerializer<String>() {
    override fun serialize(
        value: String?,
        gen: JsonGenerator?,
        serializers: SerializerProvider?
    ) {
        val text = value?.replace("/", "\\/")
        gen?.writeRawValue("\"$text\"")
    }
}

fun Route.getProgress() = get("/syncs/progress/{document}") {

    val document = call.parameters["document"]!!
    val user = call.loggedUser!!

    val data = transaction {
        Documents.selectAll().where {
            (Documents.document eq document) and (Documents.user eq user)
        }.singleOrNull()
    }

    if (data == null) {
        call.sendErrorInternal()
        return@get
    }

    val output = Output(
        data[Documents.percentage],
        data[Documents.progress],
        data[Documents.device],
        data[Documents.deviceId],
        data[Documents.timestamp],
        data[Documents.document],
    )

    // Another WTF moment.
    // Client sends header `Accept: application/vnd.koreader.v1+json` asking for a content that doesn't recognize, so it can't deserialize it.
    // So we need to force content-type to json so the client knows how to deserialize it.
    call.response.header("Content-Type", "application/json")

    call.respond(HttpStatusCode.OK, output)

}
