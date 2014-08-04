package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.AccessTokenService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.api.services.{ ItemPermissionService, PermissionService, SessionPermissionService }
import org.corespring.v2.auth._
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2.errors.Errors.{ cantFindOrgWithId, noDefaultCollection }
import org.corespring.v2.errors.V2Error
import play.api.libs.json.JsValue
import play.api.mvc._

import scalaz.Scalaz._

import scala.concurrent.ExecutionContext
import scalaz.{ Success, Failure, Validation }

/**
 * Wires up the dependencies for v2 api, so that the controllers will run in the application.
 */
class Bootstrap(
  val itemService: ItemService,
  val v1OrgService: OrganizationService,
  val accessTokenService: AccessTokenService,
  val sessionService: MongoService,
  val userService: UserService,
  val secureSocialService: SecureSocialService,
  val itemAuth: ItemAuth[OrgAndOpts],
  val sessionAuth: SessionAuth[OrgAndOpts],
  val itemTransformer: ItemTransformer,
  val headerToOrgAndOpts: RequestHeader => Validation[V2Error, OrgAndOpts]) {

  protected val orgService: OrgService = new OrgService {
    override def defaultCollection(o: Organization): Option[ObjectId] = {
      v1OrgService.getDefaultCollection(o.id) match {
        case Left(e) => None
        case Right(c) => Some(c.id)
      }
    }

    override def org(id: ObjectId): Option[Organization] = v1OrgService.findOneById(id)
  }

  protected val tokenService = new TokenService {
    override def orgForToken(token: String): Option[Organization] = {
      accessTokenService.findByToken(token).map(t => v1OrgService.findOneById(t.organization)).flatten
    }
  }

  protected val itemPermissionService: PermissionService[Organization, Item] = new ItemPermissionService {
    override def organizationService: OrganizationService = Bootstrap.this.v1OrgService
  }

  protected val sessionPermissionService: PermissionService[Organization, JsValue] = new SessionPermissionService {

  }

  private lazy val itemApi = new ItemApi {
    override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def itemService: ItemService = Bootstrap.this.itemService

    override def itemAuth: ItemAuth[OrgAndOpts] = Bootstrap.this.itemAuth

    override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = {

      val out: Validation[V2Error, String] = for {
        org <- orgService.org(identity.orgId).toSuccess(cantFindOrgWithId(identity.orgId))
        dc <- orgService.defaultCollection(org).map(_.toString()).toSuccess(noDefaultCollection(identity.orgId))
      } yield {
        dc
      }

      out match {
        case Failure(msg) =>
          logger.trace(s"Error getting default collection: $msg")
          None
        case Success(id) =>
          Some(id)
      }
    }
  }

  lazy val itemSessionApi = new ItemSessionApi {
    override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = headerToOrgAndOpts(request)

    override implicit def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def sessionAuth: SessionAuth[OrgAndOpts] = Bootstrap.this.sessionAuth

    override def sessionService = Bootstrap.this.sessionService

    override def itemTransformer = Bootstrap.this.itemTransformer
  }

  lazy val controllers: Seq[Controller] = Seq(itemApi, itemSessionApi)
}
