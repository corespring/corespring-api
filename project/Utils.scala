object Utils {
  def getEnv(prop: String): Option[String] = {
    val env = System.getenv(prop)
    if (env == null) None else Some(env)
  }
}