@file:Suppress("unused")
package es.hgg.koreader.sync.api

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadGateway
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.PaymentRequired
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class Error(val code: Int, val message: String)

// These HTTP status codes make absolutely no sense, but they are taken directly from the original sync server written in lua
// I would change them, but they are expected by the client in KOReader's sync plugin

suspend fun ApplicationCall.sendErrorDatabase() = sendError(BadGateway, 1000, "Cannot connect to redis server.")
suspend fun ApplicationCall.sendErrorInternal() = sendError(BadGateway, 2000, "Unknown server error.")
suspend fun ApplicationCall.sendErrorUnauthorized() = sendError(Unauthorized, 2001, "Unknown server error.")
suspend fun ApplicationCall.sendErrorUserExists() = sendError(PaymentRequired, 2002, "Username is already registered.")
suspend fun ApplicationCall.sendErrorInvalidRequest() = sendError(Forbidden, 2003, "Invalid Request")
suspend fun ApplicationCall.sendErrorDocumentField() = sendError(Forbidden, 2003, "Field 'document' not provided.")

suspend fun ApplicationCall.sendError(statusCode: HttpStatusCode, code: Int, message: String) =
    respond(statusCode, Error(code, message))