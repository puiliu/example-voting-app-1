package transporter

import org.json.JSONObject
import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisConnectionException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

fun main() {
    try {
        val redis: Jedis = connectToRedis("redis")
        val dbConn = connectToDB("db")
        System.err.println("Watching vote queue")
        while (true) {
            val voteJSON: String = redis.blpop(0, "votes").get(1)
            val voteData = JSONObject(voteJSON)
            val voterID: String = voteData.getString("voter_id")
            val vote: String = voteData.getString("vote")
            System.err.printf("Processing vote for '%s' by '%s'\n", vote, voterID)
            updateVote(dbConn, voterID, vote)
        }
    } catch (e: SQLException) {
        e.printStackTrace()
        System.exit(1)
    }
}

fun updateVote(dbConn: Connection?, voterID: String?, vote: String?) {
    val insert = dbConn!!.prepareStatement(
        "INSERT INTO votes (id, vote) VALUES (?, ?)"
    )
    insert.setString(1, voterID)
    insert.setString(2, vote)
    try {
        insert.executeUpdate()
    } catch (e: SQLException) {
        val update = dbConn.prepareStatement(
            "UPDATE votes SET vote = ? WHERE id = ?"
        )
        update.setString(1, vote)
        update.setString(2, voterID)
        update.executeUpdate()
    }
}

fun connectToRedis(host: String?): Jedis {
    val conn = Jedis(host)
    conn.auth("redis_password")
    while (true) {
        try {
            conn.keys("*")
            break
        } catch (e: JedisConnectionException) {
            System.err.println("Waiting for redis")
            sleep(1000)
        }
    }
    System.err.println("Connected to redis")
    return conn
}

fun connectToDB(host: String): Connection? {
    var conn: Connection? = null
    try {
        Class.forName("org.postgresql.Driver")
        val url = "jdbc:postgresql://$host/postgres"
        while (conn == null) {
            try {
                conn = DriverManager.getConnection(url, "postgres", "postgres")
            } catch (e: SQLException) {
                System.err.println("Waiting for db")
                sleep(1000)
            }
        }
        val st = conn.prepareStatement(
            "CREATE TABLE IF NOT EXISTS votes (id VARCHAR(255) NOT NULL UNIQUE, vote VARCHAR(255) NOT NULL)"
        )
        st.executeUpdate()
    } catch (e: ClassNotFoundException) {
        e.printStackTrace()
        System.exit(1)
    }
    System.err.println("Connected to db")
    return conn
}

fun sleep(duration: Long) {
    try {
        Thread.sleep(duration)
    } catch (e: InterruptedException) {
        System.exit(1)
    }
}