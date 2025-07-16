package es.hgg.koreader.sync.api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.authorizeUser() = get("/users/auth") {
    @Serializable
    data class Output(val authorized: String = "OK")
    call.respond(HttpStatusCode.OK, Output())
}

