package org.corespring.v2.auth.encryption

import org.corespring.encryption.apiClient.{ ApiClientEncryptionService, EncryptionResult }
import org.corespring.models.auth.ApiClient
import play.api.Logger
import spray.caching.Cache

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class CachingApiClientEncryptionService(underlying: ApiClientEncryptionService, timeToLive: Duration)
  extends ApiClientEncryptionService {

  private val logger = Logger(classOf[CachingApiClientEncryptionService])

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  private val decryptionCache: Cache[Option[String]] = spray.caching.LruCache(timeToLive = timeToLive)

  private def key(apiClientId: String, encrypted: String) = s"$apiClientId-$encrypted"

  override def decrypt(apiClientId: String, s: String): Option[String] = {

    logger.debug(s"function=decrypt apiClientId=$apiClientId")

    val f = decryptionCache(key(apiClientId, s)) {
      logger.trace(s"function=decrypt apiClientId=$apiClientId s=$s - calling underlying service")
      underlying.decrypt(apiClientId, s)
    }
    Await.result(f, 3.seconds)
  }

  // Use the apiClientId so that we can cache this properly
  override def decrypt(apiCilent: ApiClient, s: String): Option[String] = decrypt(apiCilent.clientId.toString, s)

  def flush = decryptionCache.clear

  /**
   * Delegate encryption functions to the underlying ApiClientEncryptionService, since they cannot be cached
   */
  override def encrypt(apiClientId: String, s: String): Option[EncryptionResult] = underlying.encrypt(apiClientId, s)
  override def encrypt(apiClient: ApiClient, s: String): Option[EncryptionResult] = underlying.encrypt(apiClient, s)
}

