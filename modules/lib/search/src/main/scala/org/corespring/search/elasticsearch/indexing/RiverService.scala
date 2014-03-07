package org.corespring.search.elasticsearch.indexing

import org.corespring.search.elasticsearch.ElasticSearchConfig
import org.corespring.platform.core.models.JsonUtil
import com.sksamuel.elastic4s.ElasticDsl._
import play.api.libs.json.{Json,JsBoolean, JsString, JsArray}

trait RiverService extends ElasticSearchConfig with JsonUtil {

  def createRiverDefinition(river: River): IndexDefinition = {
    index.into("_river", river.name).id("_meta").fields(
      "type" -> "mongodb",
      "mongodb.servers.host" -> "",
      "mongodb.servers.port" -> "",
      "mongodb.credentials.db" -> "",
      "mongodb.credentials.user" -> "",
      "mongodb.credentials.password" -> "",
      "mongodb.db" -> s"$database",
      "mongodb.collection" -> s"${river.collection}",
      "mongodb.gridfs" -> false,
      "index.name" -> river.name,
      "index.type" -> river.typ
    )
  }

  def createRiver(river: River) = {
    val source = Json.obj(
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
    ).toString

    //index: _river type: river.name id: _meta
    elasticSearchWs(s"_river/${river.name}/_meta").put(source)
  }
}
