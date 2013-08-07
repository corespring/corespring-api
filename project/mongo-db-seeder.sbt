resolvers += "Ed Eustace Mvn Repo" at "http://edeustace.com/repository/snapshots"

//resolvers += Resolver.file("Local Ivy Repository", file(Path.userHome.absolutePath+"/.ivy2/local")) (Resolver.ivyStylePatterns)

addSbtPlugin("com.ee" % "mongo-db-seeder-sbt" % "0.4-SNAPSHOT")
