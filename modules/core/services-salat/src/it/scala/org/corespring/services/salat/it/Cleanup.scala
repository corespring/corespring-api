package org.corespring.services.salat.it

import grizzled.slf4j.Logging

class Cleanup extends Logging {
  info("do cleanup")
  DbSingleton.connection.close()
}
