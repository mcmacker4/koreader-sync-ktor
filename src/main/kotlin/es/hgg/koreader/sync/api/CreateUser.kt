package es.hgg.koreader.sync.api

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import es.hgg.koreader.sync.Users
import es.hgg.koreader.sync.exists
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt.gensalt
import org.mindrot.jbcrypt.BCrypt.hashpw

fun Routing.createUser() {
    post("/users/create") {
        @Serializable
        data class UserInput(val username: String, val password: String)
        @Serializable
        data class Output(val username: String)

        val (username, password) = call.receive<UserInput>()

        withContext(Dispatchers.IO) {
            createDatabaseUser(username, password)
        }.fold(
            { call.sendErrorUserExists() },
            { call.respond(HttpStatusCode.Created, Output(username)) },
        )
    }
}

object UserExists

fun createDatabaseUser(username: String, password: String): Either<UserExists, Unit> = transaction {
    either {
        ensure(!Users.exists { Users.username eq username }) { UserExists }

        Users.insert {
            it[Users.username] = username
            it[Users.pwHash] = hashpw(password, gensalt())
        }
    }
}

