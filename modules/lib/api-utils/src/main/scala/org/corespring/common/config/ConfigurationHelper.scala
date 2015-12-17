package org.corespring.common.config

import play.api.{ Configuration }

trait ConfigurationHelper {

  protected def getString(key: String, defaultValue: Option[String] = None)(implicit config: Configuration): String = {
    config.getString(key).getOrElse(defaultValue.getOrElse(throw new RuntimeException(s"Key not found: $key")))
  }

  protected def getMaybeString(key: String)(implicit config: Configuration): Option[String] = config.getString(key)

  protected def getBoolean(key: String, defaultValue: Option[Boolean] = None)(implicit config: Configuration): Boolean = {
    config.getBoolean(key).getOrElse(defaultValue.getOrElse(throw new RuntimeException(s"Key not found: $key")))
  }

  protected def getInt(key: String, defaultValue: Option[Int] = None)(implicit config: Configuration): Int = {
    config.getInt(key).getOrElse(defaultValue.getOrElse(throw new RuntimeException(s"Key not found: $key")))
  }

}

