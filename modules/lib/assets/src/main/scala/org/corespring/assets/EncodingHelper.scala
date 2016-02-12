package org.corespring.assets

import org.apache.commons.httpclient.util.URIUtil

private[assets] class EncodingHelper {

  def isUnencoded(s: String) = {
    val decoded = URIUtil.decode(s, "utf-8")
    decoded == s
  }

  /**
   * Return a string that is only encoded once.
   * @param s
   * @return
   */
  def encodedOnce(s: String): String = {
    val decoded = decodeCompletely(s)
    URIUtil.encodePath(decoded, "utf-8")
  }

  val PlusMarker = "ENCODING_HELPER_PLUS_MARKER"

  private def swapPlus(s: String)(fn: String => String): String = {
    val swapped = s.replaceAll("\\+", PlusMarker)
    val out = fn(swapped)
    out.replaceAll(PlusMarker, "+")
  }

  def decodeCompletely(s: String): String = swapPlus(s) { s =>
    val decoded = URIUtil.decode(s, "utf-8")
    if (decoded == s) decoded else decodeCompletely(decoded)
  }
}

