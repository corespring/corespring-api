package org.corespring.api.tracking

import play.api.Logger

class LoggingTrackingService extends TrackingService {

  private val logger = Logger(classOf[LoggingTrackingService])

  override def log(c: => ApiCall): Unit = {
    logger.info(c.toKeyValues)
  }
}
