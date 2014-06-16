package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions._
import org.corespring.api.v2.services._
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.AccessTokenService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.v2.auth.{SessionBasedRequestTransformer, TokenBasedRequestTransformer}
import org.corespring.v2.auth.services.{OrgService, TokenService}
import play.api.mvc.{ Request, AnyContentAsJson, AnyContent, Controller }
import scala.Some
import scala.concurrent.ExecutionContext
import play.api.libs.json.JsValue
import scala.Some
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.services.UserService

class Bootstrap(
  val itemService: ItemService,
  val v1OrgService: OrganizationService,
  val accessTokenService: AccessTokenService,
  val sessionService: MongoService,
  val userService: UserService,
  val secureSocialService: SecureSocialService) {

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

  protected val itemPermissionService: PermissionService[Organization, Item] = new ItemPermissionService()

  protected lazy val tokenRequestTransformer: TokenBasedRequestTransformer[AnyContent] = new TokenBasedRequestTransformer[AnyContent] {
    override def orgService: OrgService = Bootstrap.this.orgService

    override def tokenService: TokenService = Bootstrap.this.tokenService
  }

  protected lazy val sessionRequestTransformer: SessionBasedRequestTransformer[AnyContent] = new SessionBasedRequestTransformer[AnyContent] {
    override def orgService: OrgService = Bootstrap.this.orgService

    override def userService: UserService = Bootstrap.this.userService

    override def secureSocialService: SecureSocialService = Bootstrap.this.secureSocialService
  }

  protected lazy val apiActions: V2ApiActions[AnyContent] =
    new CompoundAuthenticated[AnyContent] {
      override def requestTransformers: Seq[(Request[AnyContent]) => Option[OrgRequest[AnyContent]]] = Seq(
        tokenRequestTransformer.apply,
        sessionRequestTransformer.apply)

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global
    }

  private lazy val itemApi = new ItemApi {
    override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def itemService: ItemService = Bootstrap.this.itemService

    override def actions: V2ApiActions[AnyContent] = Bootstrap.this.apiActions

    override def orgService: OrgService = Bootstrap.this.orgService

    override def permissionService: PermissionService[Organization, Item] = Bootstrap.this.itemPermissionService
  }

  lazy val itemSessionApi = new ItemSessionApi {
    override def sessionService = Bootstrap.this.sessionService
  }

  lazy val controllers: Seq[Controller] = Seq(itemApi, itemSessionApi)
}
