package patches

import models.DbVersion
import controllers.Log

abstract class DbPatch{
  val version:String
  def run:Either[InternalError,Unit]
}


