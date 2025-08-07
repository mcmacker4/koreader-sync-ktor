package es.hgg.koreader.sync

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import es.hgg.koreader.sync.api.sendErrorUnauthorized
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

// This could've been an authentication provider instead,
// but it would've taken a lot more time and effort.
// This works fine.
val KOReaderAuthPlugin = createRouteScopedPlugin("KOReaderAuth") {
    onCall { call ->
        if (call.isHandled) return@onCall

        authenticateUser(call).onLeft {
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
) = createChild(TransparentRouteSelector).apply {
    install(KOReaderAuthPlugin)
    build()
}

object Unauthorized

suspend fun authenticateUser(call: PipelineCall): Either<Unauthorized, Unit> = either {
    val username = ensureNotNull(call.request.headers["x-auth-user"]) { Unauthorized }
    val password = ensureNotNull(call.request.headers["x-auth-key"]) { Unauthorized }

    val pwHash = call.async(Dispatchers.IO) { findPasswordHash(username) }.await().bind()

    ensure(BCrypt.checkpw(password, pwHash)) { Unauthorized }
}

fun findPasswordHash(username: String): Either<Unauthorized, String> = transaction {
    Users.select(Users.pwHash).where {
        Users.username eq username
    }.singleOrNull()?.let {
        it[Users.pwHash].right()
    } ?: Unauthorized.left()
}

val ApplicationCall.loggedUser get(): String? = request.headers["x-auth-user"]
