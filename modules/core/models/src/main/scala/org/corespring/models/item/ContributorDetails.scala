package org.corespring.models.item

case class ContributorDetails(
  additionalCopyrights: Seq[AdditionalCopyright] = Seq(),
  author: Option[String] = None,
  contributor: Option[String] = None,
  copyright: Option[Copyright] = None,
  costForResource: Option[Int] = None,
  credentials: Option[String] = None,
  credentialsOther: Option[String] = None,
  licenseType: Option[String] = None,
  sourceUrl: Option[String] = None)

object ContributorDetails {

  object Keys {
    val additionalCopyrights = "additionalCopyrights"
    val author = "author"
    val contributor = "contributor"
    val copyright = "copyright"
    val costForResource = "costForResource"
    val credentials = "credentials"
    val credentialsOther = "credentialsOther"
    val licenseType = "licenseType"
    val sourceUrl = "sourceUrl"
  }
}
