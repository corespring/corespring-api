package org.corespring.v2.sessiondb

/**
 * In dynamo we have one server & one db only, which holds all the tables
 * To serve multiple deployments, we add the env name as a prefix to the table names
 * eg. qa-v2.itemSessions, prod-v2.itemSessions, ...
 */
class SessionDbConfig(val sessionService: String = "mongo",
                      val sessionServiceUrl: String,
                      val sessionServiceAuthToken: String,
                      val dynamoEnvName: Option[String] = None,
                      val useLocalDynamo: Boolean,
                      val initLocalDynamo: Boolean) {

  val useDynamo = dynamoEnvName.isDefined

  private object BaseNames {
    val sessionTable = "v2.itemSessions"
    val previewSessionTable = "v2.itemSessions_preview"
  }

  private lazy val prefix = dynamoEnvName.map(e => s"$e-").getOrElse("")

  private def getName(n: String) = s"$prefix$n"
  val sessionTable = getName(BaseNames.sessionTable)
  val previewSessionTable = getName(BaseNames.previewSessionTable)

}
