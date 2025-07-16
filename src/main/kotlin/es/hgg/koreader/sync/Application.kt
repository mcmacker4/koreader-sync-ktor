package es.hgg.koreader.sync

import es.hgg.koreader.sync.api.authorizeUser
import es.hgg.koreader.sync.api.createUser
import es.hgg.koreader.sync.api.progress.getProgress
import es.hgg.koreader.sync.api.progress.updateProgress
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.receiveText
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
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
        jackson()
        jackson(ContentType.parse("application/vnd.koreader.v1+json"))
        //register(
        //    contentType = ContentType.parse("application/vnd.koreader.v1+json"),
        //    converter = JacksonConverter()
        //)
    }
}

fun Application.configureCallLogging() {
    install(DoubleReceive)
    install(CallLogging) {
        format { call ->
            runBlocking { "Body: ${call.receiveText()}" }
        }
    }
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

fun configureDatabases() {
    Database.connect(
        url = "jdbc:sqlite:data.db",
        //driver = "org.h2.Driver",
        driver = "org.sqlite.JDBC",
        user = "root",
        password = "",
    )

    transaction {
        SchemaUtils.create(Users, Documents)
    }
}