package org.corespring.v2.player

import org.corespring.container.client.ItemAssetResolver
import org.corespring.drafts.item.S3Paths
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

class SignedItemAssetResolver(
  domain: Option[String],
  validInHours: Int,
  cdnUrlSigner: CdnUrlSigner,
  version: Option[String]) extends ItemAssetResolver {

  if(!domain.isDefined){
    throw new IllegalArgumentException("domain is not defined")
  }
  if(!domain.get.startsWith("//")){
    throw new IllegalArgumentException(s"Domain must start with two slashes. Actual domain is: ${domain.get}")
  }
  if(!(validInHours > 0)){
    throw new IllegalArgumentException("validInHours should be an Int >= 0")
  }

  override def resolve(itemId: String)(file: String): String = {
    signUrl(appendOptionalVersion(s3ObjectPath(itemId, file)))
  }

  private def signUrl(s3ObjectKey: String): String = {
    val dateValidUntil = DateTime.now().plusHours(validInHours).toDate
    cdnUrlSigner.signUrl("https:" + domain.get + "/" + s3ObjectKey, dateValidUntil)
  }

  protected def s3ObjectPath(itemId: String, file: String) = {
    val vid = VersionedId(itemId).getOrElse(throw new IllegalArgumentException(s"Invalid itemId: $itemId"))
    S3Paths.itemFile(vid, file)
  }

  protected def appendOptionalVersion(url:String):String = {
    if(version.isDefined){
      val prefix = if(url.contains('?')) "&" else "?"
      url + prefix + "version=" + version.get
    } else {
      url
    }
  }

}
