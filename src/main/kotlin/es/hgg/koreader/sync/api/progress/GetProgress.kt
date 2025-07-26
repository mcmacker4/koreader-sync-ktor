package es.hgg.koreader.sync.api.progress

import es.hgg.koreader.sync.Documents
import es.hgg.koreader.sync.api.sendErrorInternal
import es.hgg.koreader.sync.loggedUser
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

@Serializable
data class Output(
    val percentage: Float,
    val progress: String,
    val device: String,
    @SerialName("device_id")
    val deviceId: String?,
    val timestamp: Long,
    val document: String,
)

fun Route.getProgress() = get("/syncs/progress/{document}") {

    val document = call.parameters["document"]!!
    val user = call.loggedUser!!

    val data = suspendedTransactionAsync(Dispatchers.IO) {
        Documents.selectAll().where {
            (Documents.document eq document) and (Documents.user eq user)
        }.singleOrNull()
    }.await()

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
