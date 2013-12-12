package org.corespring.platform.core.encryption

sealed abstract class EncryptionResult

case class EncryptionSuccess(val clientId: String, val data: String, val requested: Option[String] = None) extends EncryptionResult

case class EncryptionFailure(msg: String, e: Throwable) extends EncryptionResult
