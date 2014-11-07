package common.db

import com.mongodb.casbah.MongoDB
import play.api._
import se.radley.plugin.salat.SalatPlugin

object Db {

  def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw new PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  implicit val current = play.api.Play.current

}
