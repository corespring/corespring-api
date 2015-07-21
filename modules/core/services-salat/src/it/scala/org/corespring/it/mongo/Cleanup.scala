package org.corespring.it.mongo

import grizzled.slf4j.Logging

class Cleanup extends Logging {
  info("do cleanup")
  DbSingleton.connection.close()
}
