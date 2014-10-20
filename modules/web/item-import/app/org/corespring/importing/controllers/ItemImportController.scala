package org.corespring.importing.controllers

import org.corespring.importing.ItemFileConverter
import play.api.mvc._

class ItemImportController(converter: ItemFileConverter) extends Controller {

  def upload() = Action {
    Ok("great")
  }

}