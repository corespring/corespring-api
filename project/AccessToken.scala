import sbt.Keys._
import sbt._

object AccessToken {

  val cleanup = TaskKey[Unit]("cleanup-expired-access-tokens", "remove expired access tokens")
  val cleanupTask = cleanup <<= (streams) map safeCleanupExpiredAccessTokens

  private def safeCleanupExpiredAccessTokens(s: TaskStreams): Unit = {
    s.log.error(s"deprecated: see cs-api-dt repo")
  }

}
