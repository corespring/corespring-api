import com.mongodb.casbah.MongoURI
import org.joda.time.DateTime
import sbt.Keys._
import sbt._
import com.mongodb.casbah.Imports._

object AccessToken {

  val cleanup = TaskKey[Unit]("cleanup-expired-access-tokens", "remove expired access tokens")
  val cleanupTask = cleanup <<= (streams) map safeCleanupExpiredAccessTokens

  private def safeCleanupExpiredAccessTokens(s: TaskStreams): Unit = {
    val logId = "[safeCleanupExpiredAccessTokens]"
    lazy val isRemoteCleanupAllowed = System.getProperty("allow.remote.cleanup", "false") == "true"
    val mongoUri = Utils.getEnv("ENV_MONGO_URI").getOrElse("mongodb://localhost:27017/api")

    if (isRemoteCleanupAllowed || mongoUri.contains("localhost") || mongoUri.contains("127.0.0.1")) {
      doClean(mongoUri).map { res =>
        val (countBefore, countAfter) = res.get
        s.log.info(s"$logId - Cleaning accessTokens in $mongoUri complete. Count before/after: $countBefore/$countAfter.")
      }
    } else {
      s.log.error(s"$logId - Not allowed to cleanup a remote db. Add -Dallow.remote.cleanup=true to override.")
    }

    def doClean(mongoUri: String) = {
      connect(mongoUri).map { db =>
        val query = MongoDBObject(
          "expirationDate" -> MongoDBObject(
            //minus one day so we don't run into problems
            //when the clocks of the corespring server and
            //the executing machine are different
            "$lt" -> DateTime.now().minusDays(1).toDate),
          "neverExpire" -> false)
        val accessTokens = db("accessTokens")
        val countBefore = accessTokens.count(query)
        accessTokens.remove(query)
        val countAfter = accessTokens.count(query)
        Some((countBefore, countAfter))
      }
    }

    def connect(uri: String): Option[MongoDB] = {
      try {
        val mongoUri: MongoURI = MongoURI(uri)
        mongoUri.database.map { n =>
          val connection: MongoConnection = MongoConnection(mongoUri)
          connection(n)
        }
      } catch {
        case e: Throwable => {
          s.log.error("Error: " + e.getMessage)
          None
        }
      }
    }
  }

}
