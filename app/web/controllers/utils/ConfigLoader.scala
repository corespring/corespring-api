package web.controllers.utils

import com.typesafe.config.{ConfigFactory, Config}

object ConfigLoader {

  val config : Config = ConfigFactory.load()

  val systemConfig = ConfigFactory.systemEnvironment()

  /**
   * Load a configuration variable.
   * Checks the system env and the application.conf
   * System env takes precedence
   * @param key
   * @return
   */
  def get(key:String) : Option[String] ={
    val uriOptions = List(systemConfig, config)
    val matchingConfig : Option[Config] = uriOptions.find( config => config.hasPath(key))

    matchingConfig match {
      case Some(config) =>  Some(config.getString(key))
      case None => None
    }
  }

}
