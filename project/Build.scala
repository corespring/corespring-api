import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "corespring-api"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "se.radley" %% "play-plugins-salat" % "1.1",
    "com.typesafe" %% "play-plugins-util" % "2.0.3",
    "com.typesafe" %% "play-plugins-mailer" % "2.0.4",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "securesocial" % "securesocial_2.9.1" % "2.0.7"
    //"securesocial" % "securesocial_2.9.1" % "master"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    routesImport += "se.radley.plugin.salat.Binders._",
    templatesImport += "org.bson.types.ObjectId",
    resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/",
    resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/releases/"))(Resolver.ivyStylePatterns),
    resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/snapshots/"))(Resolver.ivyStylePatterns)
  )

}
