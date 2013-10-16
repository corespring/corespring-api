resolvers += "corespring snapshot repo" at "http://repository.corespring.org/artifactory/ivy-snapshots"

credentials ++= Seq(
  Credentials(Path.userHome / ".ivy2/.credentials")
)

addSbtPlugin("org.corespring" % "mongo-db-seeder-sbt" % "0.6-ae58487")
