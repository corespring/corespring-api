package org.corespring.common.config

trait SessionDbNames {
  def sessionTable: String
  def previewSessionTable: String
}

class MongoSessionDbNames extends SessionDbNames {
  def sessionTable = "v2.itemSessions"
  def previewSessionTable = "v2.itemSessions_preview"
}

class DynamoSessionDbNames extends SessionDbNames {
  //In dynamo we have one server & one db only, which holds all the tables
  //To serve multiple deployments, we add the env name as a prefix to the table names
  //eg. qa-v2.itemSessions, prod-v2.itemSessions, ...
  private val prefix = AppConfig.envName + "-"
  def sessionTable = prefix + "v2.itemSessions"
  def previewSessionTable = prefix + "v2.itemSessions_preview"
}


object SessionDbConfig extends SessionDbNames {

  val names = {
    if(AppConfig.dynamoDbActivate){
      new DynamoSessionDbNames
    } else {
      new MongoSessionDbNames
    }
  }

  def sessionTable = names.sessionTable
  def previewSessionTable = names.previewSessionTable

}
