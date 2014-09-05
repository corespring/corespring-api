package org.corespring.v2.auth.encryption

import org.bson.types.ObjectId
import org.corespring.platform.core.encryption.{ EncryptionResult, OrgEncryptionService }
import org.corespring.v2.log.V2LoggerFactory
import spray.caching.Cache

import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, Await }

class CachingOrgEncryptionService(underlying: OrgEncryptionService, timeToLive: Duration) extends OrgEncryptionService {

  private val logger = V2LoggerFactory.getLogger("CachingOrgEncryptionService")

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  private val decryptionCache: Cache[Option[String]] = spray.caching.LruCache(timeToLive = timeToLive)

  private def key(orgId: ObjectId, encrypted: String) = s"$orgId-$encrypted"

  override def encrypt(orgId: ObjectId, s: String): Option[EncryptionResult] = {

    logger.trace(s"function=encrypt orgId=$orgId s=$s")
    val out = underlying.encrypt(orgId, s)
    out
  }

  override def decrypt(orgId: ObjectId, s: String): Option[String] = {

    val f = decryptionCache(key(orgId, s)) {
      logger.trace(s"function=decrypt orgId=$orgId s=$s - calling underlying service")
      underlying.decrypt(orgId, s)
    }

    Await.result(f, 2.seconds)

    //decryptionCache(key(orgId,s), () => Future{
    //    logger.trace(s"function=decrypt orgId=$orgId s=$s - calling underlying service")
    //   underlying.decrypt(orgId,s)
    // }), 2.seconds
    //)
  }
}

