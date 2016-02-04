package org.corespring.v2.sessiondb

import org.bson.types.ObjectId
import org.joda.time.DateTime

trait SessionReportingUnsupported extends SessionReporting {

  override def orgCount(orgId: ObjectId, mount: DateTime) =
    throw new UnsupportedOperationException("orgCount is not supported by the SessionService in use")

}
