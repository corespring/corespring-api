package org.corespring.models.item

trait Content[Id] {
  def id: Id
  def contentType: String
  def collectionId: String
}

