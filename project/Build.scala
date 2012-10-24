import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "corespring-api"
    val appVersion      = "1.0-SNAPSHOT"
 
    val ssDependencies = Seq(
      "com.typesafe" %% "play-plugins-util" % "2.0.3",
      "com.typesafe" %% "play-plugins-mailer" % "2.0.4",
      "org.mindrot" % "jbcrypt" % "0.3m"
    )

    val secureSocial = PlayProject(
        "securesocial", appVersion, ssDependencies, mainLang = SCALA, path = file("modules/securesocial")
    ).settings(
      resolvers ++= Seq(
        "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      )
    )

    val appDependencies = Seq(
      "se.radley" %% "play-plugins-salat" % "1.1"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      routesImport += "se.radley.plugin.salat.Binders._",
      templatesImport += "org.bson.types.ObjectId",
      resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/"
    ).dependsOn(secureSocial).aggregate(secureSocial)

}
