package developer.init

import play.api.Plugin

class InitAppBeforePlugins(app:play.api.Application) extends Plugin{
  override def onStart() = {
    println("----> init legacy support via the ServiceLookup")
    bootstrap.Main.initServiceLookup()
  }
}
