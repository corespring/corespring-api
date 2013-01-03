package patches

import controllers.Log


class InitPatch extends DbPatch{
  val version = "1.0.0"
  def run:Either[InternalError,Unit] = {
    Log.i("TEST PATCH")
    Right(())
  }
}
