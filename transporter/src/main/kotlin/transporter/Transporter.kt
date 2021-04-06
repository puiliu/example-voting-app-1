package transporter

import org.json.JSONObject
import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisConnectionException
import java.lang.Thread.sleep
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

const val DB_REDIS_NAME = "redis"
const val DB_REDIS_PASSWORD = "redis_password"
const val DB_POSTGRES_NAME = "db"
const val DB_POSTGRES_USER = "postgres"
const val DB_POSTGRES_PASSWORD = "postgres"

fun main() {
    try {
        val redis = connectToRedis(DB_REDIS_NAME)
        val dbConn = connectToPostgres(DB_POSTGRES_NAME)
        println("Watching vote queue")
        while (true) {
            val voteJSON = redis.blpop(0, "votes").get(1)
            val voteData = JSONObject(voteJSON)
            val voterID = voteData.getString("voter_id")
            val vote = voteData.getString("vote")
            println("Processing vote for '$vote' by '$voterID'")
            updateVote(dbConn, voterID, vote)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun updateVote(dbConn: Connection, voterId: String, vote: String) {
    val insert = dbConn.prepareStatement("INSERT INTO votes (id, vote) VALUES (?, ?)")
    insert.setString(1, voterId)
    insert.setString(2, vote)
    try {
        insert.executeUpdate()
    } catch (e: SQLException) {
        val update = dbConn.prepareStatement("UPDATE votes SET vote = ? WHERE id = ?")
        update.setString(1, vote)
        update.setString(2, voterId)
        update.executeUpdate()
    }
}

fun connectToRedis(host: String): Jedis {
    val conn = Jedis(host)
    conn.auth(DB_REDIS_PASSWORD)
    while (true) {
        try {
            conn.keys("*")
            println("Connected to $DB_REDIS_NAME")
            return conn
        } catch (e: JedisConnectionException) {
            println("Waiting for $DB_REDIS_NAME")
            sleep(1000)
        }
    }
}

fun connectToPostgres(host: String): Connection {
    Class.forName("org.postgresql.Driver")
    val url = "jdbc:postgresql://$host/postgres"
    while (true) {
        try {
            val conn = DriverManager.getConnection(url, DB_POSTGRES_USER, DB_POSTGRES_PASSWORD)
            val st = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS votes (id VARCHAR(255) NOT NULL UNIQUE, vote VARCHAR(255) NOT NULL)"
            )
            st.executeUpdate()
            println("Connected to $DB_POSTGRES_NAME")
            return conn
        } catch (e: SQLException) {
            println("Waiting for $DB_POSTGRES_NAME")
            sleep(1000)
        }
    }
}