package es.hgg.koreader.sync

import es.hgg.koreader.sync.api.sendErrorUnauthorized
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.mindrot.jbcrypt.BCrypt

// This could've been an authentication provider instead,
// but it would've taken a lot more time and effort.
// This works fine.
val KOReaderAuthPlugin = createRouteScopedPlugin("KOReaderAuth") {
    onCall { call ->
        if (call.isHandled) return@onCall

        val username = call.request.headers["x-auth-user"]
        val password = call.request.headers["x-auth-key"]

        if (username == null || password == null || !authenticate(username, password).await()) {
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

suspend fun authenticate(username: String, password: String): Deferred<Boolean> = suspendedTransactionAsync(Dispatchers.IO) {
    Users
        .select(Users.pwHash)
        .where(Users.username.eq(username))
        .singleOrNull()?.let { user ->
            BCrypt.checkpw(password, user[Users.pwHash])
        } ?: false
}

val ApplicationCall.loggedUser get(): String? = request.headers["x-auth-user"]
