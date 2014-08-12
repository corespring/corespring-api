package org.corespring.dev.tools.controllers

import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.{ Organization => OrgModel }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.PlayerOptions
import play.api.Play
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContent, Controller, Request }
import se.radley.plugin.salat.SalatPlugin

import scalaz.Scalaz._
import scalaz.{ Validation, _ }

object CustomScoring extends Controller {

  lazy val db: MongoDB = {
    val salat = Play.current.plugin[SalatPlugin]
    salat.get.db()
  }

  lazy val hasResponseProcessing = MongoDBObject(
    "data.files.content" -> MongoDBObject("$regex" -> """.*<responseProcessing .*?>.*""", "$options" -> "m"))

  var items: Seq[Item] = {

    ItemServiceWired.collection.find(hasResponseProcessing).map { dbo =>
      import com.mongodb.casbah.Implicits._
      import org.corespring.platform.core.models.mongoContext._
      com.novus.salat.grater[Item].asObject(dbo)
    }.toSeq
  }

  lazy val sessionService: MongoService = new MongoService(db("v2.itemSessions"))

  def list = DevToolsAction { r =>

    r.getQueryString("flush").map {
      case "true" => {
        items = ItemServiceWired.collection.find(hasResponseProcessing).map { dbo =>
          import com.mongodb.casbah.Implicits._
          import org.corespring.platform.core.models.mongoContext._
          com.novus.salat.grater[Item].asObject(dbo)
        }.toSeq
      }
    }
    Ok(org.corespring.dev.tools.views.html.CustomScoringList(items))
  }

  def createSessionForItem(itemId: VersionedId[ObjectId]) = DevToolsAction { r: Request[AnyContent] =>

    def mkSession(itemId: VersionedId[ObjectId]): JsValue = Json.obj(
      "itemId" -> itemId.toString,
      "isTmp" -> true)

    val out: Validation[String, (String, String, String)] = for {
      item <- ItemServiceWired.findOneById(itemId).toSuccess("No item")
      collectionId <- item.collectionId.toSuccess("No collectionId")
      o <- Success(println(s"collectionId: $collectionId"))
      org <- OrgModel.findOne(MongoDBObject("contentcolls.collectionId" -> new ObjectId(collectionId))).toSuccess("no org")
      v2SessionId <- sessionService.create(mkSession(itemId)).toSuccess("error creating session")
      client <- ApiClient.findOneByOrgId(org.id).toSuccess("No api client")
      opts <- Success(AESCrypto.encrypt(Json.stringify(Json.toJson(PlayerOptions.ANYTHING)), client.clientSecret))
    } yield {

      (v2SessionId.toString, client.clientId.toString, opts)
    }
    out match {
      case Success((sessionId, client, opts)) => {
        val call = org.corespring.container.client.controllers.routes.PlayerLauncher.playerJs
        val url = s"${call.url}?apiClient=$client&options=$opts"
        Ok(org.corespring.dev.tools.views.html.CustomScoringPlayer(url, sessionId))
      }
      case Failure(msg) => BadRequest(msg)
    }
  }
}
