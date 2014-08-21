package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.{ AccessToken, AccessTokenService }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.api.services.{ ItemPermissionService, PermissionService, SessionPermissionService }
import org.corespring.v2.auth._
import org.corespring.v2.auth.identifiers.RequestIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2.errors.Errors._
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
  val headerToOrgAndOpts: RequestIdentity[OrgAndOpts],
  val sessionCreatedHandler: Option[VersionedId[ObjectId] => Unit]) {

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

    implicit class RichBoolean(val b: Boolean) {
      def toOption[A](a: => A): Option[A] = if (b) Some(a) else None
    }

    override def orgForToken(token: String)(implicit rh: RequestHeader): Validation[V2Error, Organization] = for {
      accessToken <- AccessToken.findByToken(token).toSuccess(invalidToken(rh))
      unexpiredToken <- if (accessToken.isExpired) Failure(expiredToken(rh)) else Success(accessToken)
      org <- orgService.org(unexpiredToken.organization).toSuccess(noOrgForToken(rh))
    } yield org
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

    override def sessionCreatedForItem(itemId: VersionedId[ObjectId]): Unit = sessionCreatedHandler.map(_(itemId))
  }

  lazy val controllers: Seq[Controller] = Seq(itemApi, itemSessionApi)
}
