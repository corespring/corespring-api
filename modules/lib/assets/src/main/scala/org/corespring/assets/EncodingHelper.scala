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

  def decodeCompletely(s: String): String = {
    val decoded = URIUtil.decode(s, "utf-8")

    if (decoded == s) {
      decoded
    } else {
      decodeCompletely(decoded)
    }
  }
}

