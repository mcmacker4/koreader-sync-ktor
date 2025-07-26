package es.hgg.koreader.sync.api

import es.hgg.koreader.sync.Users
import es.hgg.koreader.sync.exists
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.mindrot.jbcrypt.BCrypt

fun Routing.createUser() {
    post("/users/create") {
        data class UserInput(val username: String, val password: String)
        data class Output(val username: String)

        val (username, password) = call.receive<UserInput>()

        if (createUser(username, password).await()) {
            call.respond(HttpStatusCode.Created, Output(username))
        } else {
            call.sendErrorUserExists()
        }
    }
}

suspend fun createUser(username: String, password: String): Deferred<Boolean> = suspendedTransactionAsync(Dispatchers.IO) {
    if (Users.exists { Users.username eq username })
        return@suspendedTransactionAsync false

    Users.insert {
        it[Users.username] = username
        it[Users.pwHash] = hashPassword(password)
    }

    true
}

fun hashPassword(password: String): String {
    val salt = BCrypt.gensalt()!!
    return BCrypt.hashpw(password, salt)!!
}