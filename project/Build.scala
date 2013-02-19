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
    "securesocial" % "securesocial_2.9.1" % "2.0.8",
    "com.github.mumoshu" %% "play2-memcached" % "0.2.3-SNAPSHOT",
    "org.mockito" % "mockito-all" % "1.9.5",

    //Assets Loader - need to add the google js compiler too - its part of the play framework but only at build time.
    "com.ee" %% "assets-loader" % "0.4-SNAPSHOT",
    ("com.google.javascript"            %    "closure-compiler"         %   "rr2079.1" notTransitive())
      .exclude("args4j", "args4j")
      .exclude("com.google.guava", "guava")
      .exclude("org.json", "json")
      .exclude("com.google.protobuf", "protobuf-java")
      .exclude("org.apache.ant", "ant")
      .exclude("com.google.code.findbugs", "jsr305")
      .exclude("com.googlecode.jarjar", "jarjar")
      .exclude("junit", "junit")
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    //because of all the db testing we need - only test serially
    parallelExecution.in(Test) := false,
    routesImport += "se.radley.plugin.salat.Binders._",
    templatesImport += "org.bson.types.ObjectId",
    resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/",
    resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/releases/"))(Resolver.ivyStylePatterns),
    resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/snapshots/"))(Resolver.ivyStylePatterns),
    resolvers += "Sonatype OSS Snapshots Repository" at "http://oss.sonatype.org/content/groups/public",
    resolvers += "Spy Repository" at "http://files.couchbase.com/maven2", // required to resolve `spymemcached`, the plugin's dependency.
    resolvers += "ed eustace repo" at "http://edeustace.com/repository/releases",
    resolvers += "ed eustace snapshots repo" at "http://edeustace.com/repository/snapshots"

  )

}
