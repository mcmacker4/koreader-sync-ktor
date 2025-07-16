package es.hgg.koreader.sync.api

import es.hgg.koreader.sync.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

fun Routing.createUser() {
    post("/users/create") {
        @Serializable
        data class UserInput(val username: String, val password: String)
        @Serializable
        data class Output(val username: String)

        val (username, password) = call.receive<UserInput>()

        if (createUser(username, password)) {
            call.respond(HttpStatusCode.Created, Output(username))
        } else {
            call.sendErrorUserExists()
        }
    }
}

fun createUser(username: String, password: String): Boolean = transaction {
    val count = Users
        .select(Users.username)
        .where(Users.username.eq(username))
        .count()

    if (count > 0)
        return@transaction false

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