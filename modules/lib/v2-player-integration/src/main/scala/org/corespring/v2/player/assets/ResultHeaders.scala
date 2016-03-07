package org.corespring.v2.player.assets

import play.api.mvc.SimpleResult

/**
 * Exposes an implicit ResultWithHeaders#withContentHeaders which will handle adding appropriate content headers (such
 * as 'Content-Type' and 'Content-Encoding' based on a provided path.
 */
trait ResultHeaders {

  private val headerMap = Map(
    "svgx" -> Seq("Content-Type" -> "image/svg+xml", "Content-Encoding" -> "gzip", "Vary" -> "Accept-Encoding")
  )

  implicit class ResultWithHeaders(result: SimpleResult) {

    def withContentHeaders(path: String): SimpleResult = {
      val extension = path.split("\\.").last
      println(extension)
      println(headerMap.get(extension))
      headerMap.get(extension).map(headers => result.withHeaders(headers:_*)).getOrElse(result)
    }

  }

}
