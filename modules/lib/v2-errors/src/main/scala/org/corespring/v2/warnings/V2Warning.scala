package org.corespring.v2.warnings

sealed abstract class V2Warning(val message: String, val code: String)

private[v2] object Warnings {

  case class deprecatedQueryStringParameter(deprecatedParam: String, useParam: String) extends V2Warning(
    s"deprecated query string parameter in use: $deprecatedParam, use $useParam instead",
    "DEPRECATED_QUERY_STRING_PARAM")
}
