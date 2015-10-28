package org.corespring.models.item

case class AdditionalCopyright(author: Option[String] = None,
  owner: Option[String] = None,
  year: Option[String] = None,
  licenseType: Option[String] = None,
  mediaType: Option[String] = None,
  sourceUrl: Option[String] = None,
  costForResource: Option[Int] = None)

