package org.corespring.common.config

import org.bson.types.ObjectId
import play.api.Configuration
import scala.language.implicitConversions

class AppConfig(config: Configuration) {

  protected object Key extends Enumeration {
    type Key = Value
    val DEMO_ORG_ID, ROOT_ORG_ID, V2_PLAYER_ORG_IDS, AMAZON_ACCESS_SECRET, AMAZON_ACCESS_KEY, AMAZON_ASSETS_BUCKET = Value
  }

  private implicit def keyToString(k: Key.Key): String = k.toString

  lazy val demoOrgId: ObjectId = config.getString(Key.DEMO_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("Not found"))
  lazy val rootOrgId: ObjectId = config.getString(Key.ROOT_ORG_ID).map(new ObjectId(_)).getOrElse(throw new RuntimeException("Not found"))
  lazy val assetsBucket: String = config.getString(Key.AMAZON_ASSETS_BUCKET).getOrElse("?")
  lazy val amazonKey: String = config.getString(Key.AMAZON_ACCESS_KEY).getOrElse("?")
  lazy val amazonSecret: String = config.getString(Key.AMAZON_ACCESS_SECRET).getOrElse("?")
  lazy val v2playerOrgIds: Seq[ObjectId] = config.getString(Key.V2_PLAYER_ORG_IDS)
    .map(_.split(",").map(new ObjectId(_)).toSeq).getOrElse(Seq.empty[ObjectId])
}

object AppConfig extends AppConfig(play.api.Play.current.configuration)
