package org.corespring.common.config

import play.api.{ Configuration }


trait ConfigurationHelper {

  protected def getString(key: String)(implicit config: Configuration): String = {
    config.getString(key).getOrElse(throw new RuntimeException(s"Key not found: $key"))
  }

  protected def getString(key: String, defaultValue: String)(implicit config: Configuration): String = {
    config.getString(key).getOrElse(defaultValue)
  }

  protected def getMaybeString(key: String)(implicit config: Configuration): Option[String] = config.getString(key)

  protected def getBoolean(key: String, defaultValue: Boolean)(implicit config: Configuration): Boolean = {
    config.getBoolean(key).getOrElse(defaultValue)
  }

  protected def getBoolean(key: String)(implicit config: Configuration): Boolean = {
    config.getBoolean(key).getOrElse(throw new RuntimeException(s"Key not found: $key"))
  }

  protected def getInt(key: String, defaultValue: Int)(implicit config: Configuration): Int = {
    config.getInt(key).getOrElse(defaultValue)
  }

  protected def getInt(key: String)(implicit config: Configuration): Int = {
    config.getInt(key).getOrElse(throw new RuntimeException(s"Key not found: $key"))
  }

}

