package org.corespring.common.config

class MongoSessionDbNames {
  def sessionTable = "v2.itemSessions"
  def previewSessionTable = "v2.itemSessions_preview"
}

class DynamoSessionDbNames {
  private val prefix = AppConfig.envName + "-"
  def sessionTable = prefix + "v2.itemSessions"
  def previewSessionTable = prefix + "v2.itemSessions_preview"
}


object SessionDbConfig extends DynamoSessionDbNames {

}
