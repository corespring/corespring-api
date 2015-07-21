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

