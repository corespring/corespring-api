package org.corespring.container.client.controllers.launcher

import java.io.File

trait JsResource {

  def load: String => Option[String]

  /**
   * Read a js resource from the classpath
   *
   * @param p
   * @return name (without suffix) -> source
   */
  def pathToNameAndContents(p: String): (String, String) = {
    load(p).map { contents =>
      import grizzled.file.GrizzledFile._
      val name = new File(p).basename.getName.replace(".js", "")
      name -> contents
    }.getOrElse {
      throw new RuntimeException(s"Can't find resource for path: $p")
    }
  }
}

