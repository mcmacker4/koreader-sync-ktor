package es.hgg.koreader.sync.api.progress

import com.fasterxml.jackson.annotation.JsonProperty
import es.hgg.koreader.sync.Documents
import es.hgg.koreader.sync.loggedUser
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.upsert

fun Route.updateProgress() = put("/syncs/progress") {

    data class DocumentInput(
        val progress: String,
        val document: String,
        @param:JsonProperty("device_id") val deviceId: String?,
        val device: String,
        val percentage: Float,
    )

    data class Output(val document: String, val timestamp: Long)

    val timestamp = java.time.Instant.now().epochSecond

    val input = call.receive<DocumentInput>()
    println("Input: $input")

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

    val output = Output(input.document, timestamp)
    println("Output: $output")
    call.respond(HttpStatusCode.OK, output)

}