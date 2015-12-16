package org.corespring.v2.player

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.PrivateKey

import com.amazonaws.auth.PEM
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner
import org.corespring.container.client.ItemAssetResolver
import org.corespring.drafts.item.S3Paths
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

class CdnItemAssetResolver(cdnResolver: CDNResolver, signUrls: Boolean, keyPairId: Option[String], privateKeyString: Option[String]) extends ItemAssetResolver {

  lazy val privateKey: PrivateKey = {
    val s = privateKeyString.get
    PEM.readPrivateKey(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)))
  }

  override def resolve(itemId: String)(file: String): String = {
    val path = s3ObjectPath(itemId, file)
    if (signUrls && cdnResolver.cdnDomain.isDefined && keyPairId.isDefined && privateKeyString.isDefined) {
      signUrl(cdnResolver.cdnDomain.get, path, keyPairId.get, privateKey)
    } else {
      cdnResolver.resolveDomain(path)
    }
  }

  private def signUrl(cdnDomain: String, s3ObjectKey: String, keyPairId: String, privateKey: PrivateKey) = {
    val protocol = "https:"
    val dateLessThan = DateTime.now().plusDays(1).toDate

    CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
      protocol + cdnDomain + "/" + s3ObjectKey,
      keyPairId,
      privateKey,
      dateLessThan)
  }

  protected def s3ObjectPath(itemId: String, file: String) = {
    val vid = VersionedId(itemId).getOrElse(throw new RuntimeException(s"Invalid itemId: $itemId"))
    S3Paths.itemFile(vid, file)
  }

}
