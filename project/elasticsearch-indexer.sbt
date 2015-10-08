resolvers ++= Seq("cs snapshots" at "http://repository.corespring.org/artifactory/ivy-snapshots",
  "cs releases" at "http://repository.corespring.org/artifactory/ivy-releases")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

addSbtPlugin("org.corespring" % "elasticsearch-indexer-sbt" % "0.0.9")
