package org.corespring.platform.core.models.json

case class JsonValidationException(field: String) extends RuntimeException("invalid value for: " + field)
