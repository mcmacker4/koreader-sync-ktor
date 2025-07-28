package es.hgg.koreader.sync

import es.hgg.koreader.sync.api.authorizeUser
import es.hgg.koreader.sync.api.createUser
import es.hgg.koreader.sync.api.progress.getProgress
import es.hgg.koreader.sync.api.progress.updateProgress
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureCallLogging()
    configureDatabases()
    configureRouting()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
        json(contentType = ContentType.parse("application/vnd.koreader.v1+json"))
    }
}

fun Application.configureCallLogging() {
    install(CallLogging)
}

fun Application.configureRouting() {
    routing {
        createUser()
        authorizeUser()
        authenticate {
            getProgress()
            updateProgress()
        }
    }
}

fun Application.configureDatabases() {
    val url = environment.config.property("jdbc.url").getString()
    val driver = environment.config.property("jdbc.driver").getString()
    val user = environment.config.propertyOrNull("jdbc.user")?.getString() ?: ""
    val password = environment.config.propertyOrNull("jdbc.password")?.getString() ?: ""

    Database.connect(url = url, driver = driver, user = user, password = password)

    transaction {
        SchemaUtils.create(Users, Documents)
    }
}