import sbt._
import sbt.Keys._

object NewRelic {

  private val newRelicConfig = config("new-relic").hide
  private val newRelic = "com.newrelic.agent.java" % "newrelic-agent" % "3.25.0" % newRelicConfig

  val settings = Seq(
    ivyConfigurations += newRelicConfig,
    libraryDependencies ++= Seq(newRelic),
    unmanagedClasspath in Runtime ++= update.value.select(configurationFilter("new-relic")))
}
