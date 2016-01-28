addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")

credentials += Credentials(Path.userHome / ".ivy2"/".credentials")
resolvers += "corespring-ivy-releases" at "http://repository.corespring.org/artifactory/ivy-releases"
addSbtPlugin("org.corespring" % "sbt-release-extras" % "1.19.0")
