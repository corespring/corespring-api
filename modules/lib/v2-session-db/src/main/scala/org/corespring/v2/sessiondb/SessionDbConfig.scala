package org.corespring.v2.sessiondb

/**
 * In dynamo we have one server & one db only, which holds all the tables
 * To serve multiple deployments, we add the env name as a prefix to the table names
 * eg. qa-v2.itemSessions, prod-v2.itemSessions, ...
 */
class SessionDbConfig(dynamoEnvName: Option[String] = None) {

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
