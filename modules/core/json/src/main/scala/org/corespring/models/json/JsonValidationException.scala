package org.corespring.models.json

case class JsonValidationException(field: String) extends RuntimeException("invalid value for: " + field)

