package org.corespring.models.item

case class Copyright(owner: Option[String] = None,
  year: Option[String] = None,
  expirationDate: Option[String] = None,
  imageName: Option[String] = None)

object Copyright {
  object Keys {
    val owner = "owner"
    val year = "year"
    val expirationDate = "expirationDate"
    val imageName = "imageName"
  }
}

