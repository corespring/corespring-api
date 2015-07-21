package org.corespring.models.item

case class Copyright(owner: Option[String] = None,
  year: Option[String] = None,
  expirationDate: Option[String] = None,
  imageName: Option[String] = None)

