package web.controller.utils

object ConfigLoader {

  import play.api.Play.current


  /**
   * Load a configuration variable.
   * Checks the system env and the application.conf
   * System env takes precedence
   * @param key
   * @return
   */
  def get(key:String) : Option[String] ={
    try{
      current.configuration.getString(key)
    }
    catch {
      case _ : Throwable => None
    }
  }
}
