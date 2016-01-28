package org.corespring.web.common.views.helpers

import java.io.InputStream
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

  def apply(resourceAsStream: String => Option[InputStream]): BuildInfo = {
    val propsFile = "/buildInfo.properties"

    val properties = {
      resourceAsStream(propsFile).map { is =>
        val props = new Properties()
        props.load(is)
        is.close()
        props
      }.getOrElse(new Properties())
    }

    BuildInfo(
      version = properties.getProperty("version", "?"),
      commitHashShort = properties.getProperty("commit.hash", "?"),
      pushDate = properties.getProperty("date", "?"),
      branch = properties.getProperty("branch", "?"))
  }
}

