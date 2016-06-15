package org.corespring.v2.sessiondb

import org.bson.types.ObjectId
import org.joda.time.DateTime

/**
 * Defines methods used to report session data
 */
trait SessionReporting {

  def orgCount(orgId: ObjectId, mount: DateTime): Option[Map[DateTime, Long]]

}
