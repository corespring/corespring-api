import sbt._

object LoadCredentials {

  import Utils._

  lazy val cred: Credentials = {
    val envCredentialsPath = getEnv("CREDENTIALS_PATH")
    val path = envCredentialsPath.getOrElse(Seq(Path.userHome / ".ivy2" / ".credentials").mkString)
    val f: File = file(path)
    println("[credentials] check file: : " + f.getAbsolutePath)
    if (f.exists()) {
      println("[credentials] using credentials file")
      Credentials(f)
    } else {
      //https://devcenter.heroku.com/articles/labs-user-env-compile
      println("[credentials] using credentials env vars - you need to have: user-env-compile enabled in heroku")

      def repoVar(s: String) = System.getenv("ARTIFACTORY_" + s)
      val args = Seq("REALM", "HOST", "USER", "PASS").map(repoVar)
      println("[credentials] args: " + args)
      Credentials(args(0), args(1), args(2), args(3))
    }
  }
}