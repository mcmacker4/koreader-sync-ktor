package es.hgg.koreader.sync

import es.hgg.koreader.sync.api.sendErrorUnauthorized
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

val KOReaderAuthPlugin = createRouteScopedPlugin("KOReaderAuth") {
    onCall { call ->
        if (call.isHandled) return@onCall

        val username = call.request.headers["x-auth-user"]
        val password = call.request.headers["x-auth-key"]

        if (username == null || password == null || !authenticate(username, password)) {
            call.sendErrorUnauthorized()
        }
    }
}

object TransparentRouteSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Transparent
    }

    override fun toString(): String = "(authenticate)"
}

fun Route.authenticate(
    build: Route.() -> Unit
): Route {
    val child = createChild(TransparentRouteSelector)
    child.install(KOReaderAuthPlugin)
    child.build()
    return child
}

fun authenticate(username: String, password: String): Boolean = transaction {
    Users
        .select(Users.pwHash)
        .where(Users.username.eq(username))
        .singleOrNull()?.let { user ->
            BCrypt.checkpw(password, user[Users.pwHash])
        } ?: false
}

val ApplicationCall.loggedUser get(): String? = request.headers["x-auth-user"]
