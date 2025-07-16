package es.hgg.koreader.sync

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val username = varchar("username", 60)
    val pwHash = varchar("pw_hash", 60)

    override val primaryKey = PrimaryKey(username)
}

object Documents : Table("documents") {
    val user = varchar("username", 60)
    val document = varchar("document", 200)

    val percentage = float("percentage")
    val progress = varchar("progress", 200)

    val deviceId = varchar("deviceId", 200).nullable()
    val device = varchar("device", 200)

    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(user, document)

    init {
        foreignKey(user to Users.username)
    }
}