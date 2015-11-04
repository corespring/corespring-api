package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ItemMetadataHooks => ContainerMetadataHooks}
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.models.auth.Permission
import org.corespring.models.json.metadata.SetJson
import org.corespring.models.{ContentCollection, Organization}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.metadata.{MetadataSetService, MetadataService}
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz._

class ItemMetadataHooks(
  metadataService: MetadataService,
  metadataSetService: MetadataSetService,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val containerContext: ContainerExecutionContext) extends ContainerMetadataHooks with LoadOrgAndOptions {

  lazy val logger = Logger(classOf[ItemMetadataHooks])

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  override def get(id:String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {
    println("joco"+id)

    getOrg(header) match {
      case Success(org) =>
        println("org "+org.id)
        val sets = metadataSetService.list(org.id)
        println(sets)
        val vid = VersionedId(id).get
        println(vid)
        val metadata = metadataService.get(vid, sets.map(_.metadataKey))
        println(metadata)
        val setAndData = sets.map(s => (s, metadata.find(_.key == s.metadataKey)))
        println(setAndData)
        Right(Json.toJson(setAndData.map(t => SetJson(t._1, t._2))))
      case Failure(_) =>
        val error = generalError(s"couldn't get metadata for item $id")
        Left(error.statusCode -> error.message)
    }
  }

  private def getOrg(h: RequestHeader): Validation[V2Error, Organization] = {
    getOrgAndOptions(h).map { o =>
      o.org
    }
  }

}

