package org.corespring.assets

import org.apache.commons.httpclient.util.URIUtil

private[assets] class EncodingHelper {

  /**
   * Return a string that is only encoded once.
   * @param s
   * @return
   */
  def encodedOnce(s: String): String = {
    val decoded = decodeCompletely(s)
    URIUtil.encodeQuery(decoded, "utf-8")
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

