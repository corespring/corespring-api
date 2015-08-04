package org.corespring.v2.sessiondb

trait SessionServiceFactory {
  def create(tableName: String): SessionService
}

