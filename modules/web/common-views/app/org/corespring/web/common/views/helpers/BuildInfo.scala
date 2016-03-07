package org.corespring.web.common.views.helpers

import java.io.{ StringReader, InputStream }
import java.util.Properties

import com.typesafe.config.{ Config, ConfigFactory }
import org.corespring.models.item.{ ComponentType, FieldValue }
import play.api.libs.json._

case class BuildInfo(commitHashShort: String, pushDate: String, branch: String, version: String) {
  lazy val json = Json.obj(
    "version" -> version,
    "commitHash" -> commitHashShort,
    "branch" -> branch,
    "date" -> pushDate)
}

object BuildInfo {

  def apply(resourceAsString: String => Option[String]): BuildInfo = {
    val propsFile = "/buildInfo.properties"

    val properties = resourceAsString(propsFile).map { s =>
      val props = new Properties()
      props.load(new StringReader(s))
      props
    }.getOrElse(new Properties())

    BuildInfo(
      version = properties.getProperty("version", "?"),
      commitHashShort = properties.getProperty("commit.hash", "?"),
      pushDate = properties.getProperty("date", "?"),
      branch = properties.getProperty("branch", "?"))
  }
}

