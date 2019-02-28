package org.corespring.container.client.io

import java.net.URL
import java.nio.charset.StandardCharsets

import org.apache.commons.io.IOUtils

class ResourcePath(loadURL: String => Option[URL]) {

  def lastModified(p: String): Option[Long] = {
    loadURL(p: String).flatMap { url =>
      try {
        Some(url.openConnection().getLastModified)
      } catch {
        case _: Throwable => None
      }
    }
  }

  def loadPath(path: String): Option[String] = {
    loadURL(path).map { url =>
      try {
        IOUtils.toString(url, StandardCharsets.UTF_8)
      } catch {
        case e: Throwable => throw new RuntimeException(s"Error converting path: $path to string", e)
      }
    }
  }
}

