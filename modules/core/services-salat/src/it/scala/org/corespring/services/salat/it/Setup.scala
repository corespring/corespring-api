package org.corespring.services.salat.it

import grizzled.slf4j.Logging

class Setup extends Logging {
  info("-> Setup")
  DbSingleton.db
}
