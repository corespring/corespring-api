package org.corespring.models.item

case class ComponentType(componentType: String, label: String) {
  def tuple = componentType -> label
}