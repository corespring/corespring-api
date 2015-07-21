package org.corespring.it.mongo

import grizzled.slf4j.Logging

class Setup extends Logging{
    info("-> Setup")
    DbSingleton.db
}
