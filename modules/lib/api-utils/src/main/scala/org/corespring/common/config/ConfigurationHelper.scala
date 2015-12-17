package org.corespring.common.config

import play.api.{Configuration}

trait ConfigurationHelper {

  def config: Configuration

  protected def getString(key: String, defaultValue: Option[String] = None): String = {
    config.getString(key).getOrElse(defaultValue.getOrElse(throw new RuntimeException(s"Key not found: $key")))
  }

  protected def getMaybeString(key: String) = config.getString(key)

  protected def getBoolean(key: String, defaultValue: Option[Boolean] = None): Boolean = {
    config.getBoolean(key).getOrElse(defaultValue.getOrElse(throw new RuntimeException(s"Key not found: $key")))
  }

  protected def getInt(key: String, defaultValue: Option[Int] = None): Int = {
    config.getInt(key).getOrElse(defaultValue.getOrElse(throw new RuntimeException(s"Key not found: $key")))
  }

}

