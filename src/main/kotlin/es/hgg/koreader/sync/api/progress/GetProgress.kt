package es.hgg.koreader.sync.api.progress

import arrow.core.left
import arrow.core.right
import es.hgg.koreader.sync.Documents
import es.hgg.koreader.sync.api.sendErrorInternal
import es.hgg.koreader.sync.loggedUser
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class DocumentInfo(
    val percentage: Float,
    val progress: String,
    val device: String,
    @SerialName("device_id")
    val deviceId: String?,
    val timestamp: Long,
    val document: String,
)

object DocumentNotFound

fun Route.getProgress() = get("/syncs/progress/{document}") {

    val document = call.parameters["document"]!!
    val user = call.loggedUser!!

    findDocument(user, document).fold(
        { call.sendErrorInternal() },
        { document ->
            // Another WTF moment.
            // Client sends header `Accept: application/vnd.koreader.v1+json` asking for a content that doesn't recognize, so it can't deserialize it.
            // So we need to force content-type to json so the client knows how to deserialize it.
            call.response.header("Content-Type", "application/json")
            call.respond(HttpStatusCode.OK, document)
        },
    )
}

suspend fun findDocument(user: String, document: String) = withContext(Dispatchers.IO) {
    transaction {
        Documents.selectAll()
            .where { (Documents.document eq document) and (Documents.user eq user) }
            .map { it.intoDocument().right() }
            .singleOrNull() ?: DocumentNotFound.left()
    }
}

fun ResultRow.intoDocument() = DocumentInfo(
    this[Documents.percentage],
    this[Documents.progress],
    this[Documents.device],
    this[Documents.deviceId],
    this[Documents.timestamp],
    this[Documents.document],
)
