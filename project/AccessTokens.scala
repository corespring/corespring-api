import com.mongodb.casbah.MongoURI
import org.joda.time.DateTime
import play.Play
import sbt.Keys._
import sbt._
import com.mongodb.casbah.Imports._


object AccessTokens {

  val cleanup = TaskKey[Unit]("cleanup-expired-access-tokens")
  val cleanupTask = cleanup <<= (streams) map safeCleanupExpiredAccessTokens

  private def safeCleanupExpiredAccessTokens(s: TaskStreams): Unit = {
    val logId = "[safeCleanupExpiredAccessTokens]"
    lazy val isRemoteCleanupAllowed = System.getProperty("allow.remote.cleanup", "false") == "true"
    val mongoUri = Utils.getEnv("ENV_MONGO_URI").getOrElse("mongodb://localhost:27017/api")
    if (isRemoteCleanupAllowed || mongoUri.contains("localhost") || mongoUri.contains("127.0.0.1")) {
      doClean(mongoUri).map { res =>
        val (countBefore, countAfter) = res.get
        info(s"$logId - Cleaning accessTokens in $mongoUri complete. Count before/after: $countBefore/$countAfter.")
      }
    } else {
      error(
        s"$logId - Not allowed to cleanup a remote db. Add -Dallow.remote.cleanup=true to override.")
    }
  }

  private def doClean(mongoUri: String) = {
    connect(mongoUri).map { db =>
      val query = MongoDBObject(
        "expirationDate" -> MongoDBObject(
          "$lt" -> DateTime.now().toDate
        ),
        "neverExpire" -> false
      )
      val accessTokens = db("accessTokens")
      val countBefore = accessTokens.count(query)
      accessTokens.remove(query)
      val countAfter = accessTokens.count(query)
      Some((countBefore, countAfter))
    }
  }

  private def connect(uri: String): Option[MongoDB] = {
    try {
      val mongoUri: MongoURI = MongoURI(uri)
      mongoUri.database.map { n =>
        val connection: MongoConnection = MongoConnection(mongoUri)
        connection(n)
      }
    } catch {
      case e: Throwable => {
        error("Error: " + e.getMessage)
        None
      }
    }
  }

  private def info(s:String){
    println(s"INFO [safeCleanupExpiredAccessTokens] - $s")
  }

  private def error(s:String){
    println(s"ERROR [safeCleanupExpiredAccessTokens] - $s")
  }
}
