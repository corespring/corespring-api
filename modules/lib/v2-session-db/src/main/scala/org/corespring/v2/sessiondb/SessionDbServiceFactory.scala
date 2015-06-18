package org.corespring.v2.sessiondb

trait SessionDbServiceFactory {
  def create(tableName: String): SessionDbService
}


