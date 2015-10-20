package org.corespring.models.json

case class JsonValidationException(field: String) extends RuntimeException("invalid value for: " + field)

case class UnacceptableJsonValueException(field: String, value: String, acceptable: Seq[String]) extends RuntimeException(
  s"Unacceptable value '$value' for field: '$field', must be one of: ${acceptable.mkString(", ")}")
