package es.hgg.koreader.sync.api.progress

import es.hgg.koreader.sync.Documents
import es.hgg.koreader.sync.loggedUser
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.upsert

fun Route.updateProgress() = put("/syncs/progress") {

    @Serializable
    data class DocumentInput(
        val progress: String,
        val document: String,
        @SerialName("device_id")
        val deviceId: String?,
        val device: String,
        val percentage: Float,
    )

    @Serializable
    data class Output(val document: String, val timestamp: Long)

    val timestamp = java.time.Instant.now().epochSecond

    val input = call.receive<DocumentInput>()

    // Should only be reached when authenticated
    val user = call.loggedUser!!

    suspendedTransactionAsync(Dispatchers.IO) {

        Documents.upsert {
            it[Documents.user] = user
            it[Documents.document] = input.document

            it[Documents.progress] = input.progress
            it[Documents.percentage] = input.percentage
            it[Documents.device] = input.device
            it[Documents.deviceId] = input.deviceId

            it[Documents.timestamp] = timestamp
        }

    }.await()

    call.respond(HttpStatusCode.OK, Output(input.document, timestamp))

}