package es.hgg.koreader.sync.api.progress

import es.hgg.koreader.sync.Documents
import es.hgg.koreader.sync.loggedUser
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.Instant

@Serializable
data class Output(val document: String, val timestamp: Long)

@Serializable
data class DocumentInput(
    val progress: String,
    val document: String,
    @SerialName("device_id")
    val deviceId: String?,
    val device: String,
    val percentage: Float,
)

fun Route.updateProgress() = put("/syncs/progress") {
    val input = call.receive<DocumentInput>()

    // Should only be reached when authenticated
    val user = call.loggedUser!!

    val timestamp = Instant.now().epochSecond
    call.launch(Dispatchers.IO) { updateDatabase(user, input, timestamp) }.join()

    call.respond(HttpStatusCode.OK, Output(input.document, timestamp))
}

fun updateDatabase(user: String, input: DocumentInput, timestamp: Long) {
    transaction {
        Documents.upsert {
            it[Documents.user] = user
            it[Documents.document] = input.document

            it[Documents.progress] = input.progress
            it[Documents.percentage] = input.percentage
            it[Documents.device] = input.device
            it[Documents.deviceId] = input.deviceId

            it[Documents.timestamp] = timestamp
        }
    }
}