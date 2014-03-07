package org.corespring.search.elasticsearch.indexing

import org.corespring.search.elasticsearch.ElasticSearchConfig
import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._
import play.api.libs.json.JsString
import play.api.libs.json.JsArray
import scala.Some
import play.api.libs.json.JsBoolean

trait RiverConfigUtil extends ElasticSearchConfig with JsonUtil {

  def createRiverConfigJson(river: River): JsObject = {
    Json.obj(
      "type" -> "mongodb",
      "mongodb" -> partialObj(
        "servers" -> Some(JsArray(mongoHosts.map(host => Json.obj(
          "host" -> host.split(":").head,
          "port" -> host.split(":").last
        )))),
        "credentials" -> (mongoCredentials() match {
          case (Some(username), Some(password)) => {
            Some(Json.obj(
              "db" -> database,
              "user" -> username,
              "password" -> password.toString
            ))
          }
          case _ => None
        }),
        "db" -> Some(JsString(database)),
        "collection" -> Some(JsString(river.collection)),
        "gridfs" -> Some(JsBoolean(false))
      ),
      "index" -> Json.obj(
        "name" -> river.name,
        "type" -> river.typ
      )
    )
  }

}
