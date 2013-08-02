package tests.helpers

import common.encryption.ShaHash
import controllers.auth.{OAuthConstants, Permission}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.execute.{Failure, Result}
import org.specs2.mutable.After
import securesocial.core.{UserId, Authenticator, SecureSocial}
import play.api.Logger
import developer.controllers.{CoreSpringUserService, Developer}
import play.api.mvc.Cookie
import play.api.cache.Cache
import org.corespring.platform.core.models.{User, Organization, ContentCollection}
import org.corespring.platform.core.models.auth.AccessToken

trait TestModelHelpers {

  class TestOPlenty(p: Permission, collectionPermission: Permission = Permission.None) extends After {

    private lazy val testOrgToUse = testOrg
    lazy val org: Organization = createOrg
    lazy val user: User = createUser
    lazy val tokenId: String = createToken
    lazy val collection: ContentCollection = createCollection

    def createOrg = Organization.insert(testOrgToUse, None) match {
      case Right(o) => o
      case Left(error) => throw new RuntimeException(error.message)
    }

    def createUser = User.insertUser(testUser, org.id, p) match {
      case Right(u) => u
      case Left(e) => throw new RuntimeException(e.message)
    }

    def createToken = AccessToken.insert(new AccessToken(org.id, Some(user.userName), "testuser_token")) match {
      case Some(token) => "testuser_token"
      case _ => throw new RuntimeException("no token created")
    }

    def createCollection = ContentCollection.insertCollection(org.id, new ContentCollection("test_collection"), collectionPermission) match {
      case Right(coll) => coll
      case Left(e) => throw new RuntimeException("no collection created")
    }

    def after {
      Logger(this.getClass.getName).debug("removing AccessToken, User, Organization + ContentCollection")
      AccessToken.removeToken(tokenId)
      User.remove(user)
      Organization.remove(org)
      ContentCollection.remove(collection)
    }
  }

  /** Execute a Specs method body once we successfully insert the org, also remove Org afterwards */
  def withOrg(org: Organization, fn: Organization => Result, maybeParentId: Option[ObjectId] = None): Result = {
    Organization.insert(org, maybeParentId) match {
      case Right(o) => {
        val result = fn(o)
        Organization.delete(o.id)
        result
      }
      case Left(error) => Failure(error.message)
    }
  }

  /** Execute a specs method body once we successfully insert a User, also remove user afterwards */
  def withUser(user: User, orgId: ObjectId, p: Permission, fn: User => Result): Result = {
    User.insertUser(user, orgId, p) match {
      case Right(u) => {
        val result = fn(u)
        User.removeUser(u.id)
        result
      }
      case Left(error) => Failure(error.message)
    }
  }

  def testUser = new User("testoplenty")

  def testOrg = new Organization("test")

  def secureSocialCookie(u: Option[User], expires : Option[DateTime] = None): Option[Cookie] = u.map{ user =>

      val authenticator : Authenticator = Authenticator.create(CoreSpringUserService.toIdentity(user)) match {
        case Left(e) => throw new RuntimeException(e)
        case Right(a) => a
      }
      authenticator.toCookie
  }

  def expiredSecureSocialCookie(u: Option[User]): Option[Cookie] = u.map{ user =>

    val authenticator : Authenticator = Authenticator.create(CoreSpringUserService.toIdentity(user)) match {
      case Left(e) => throw new RuntimeException(e)
      case Right(a) => a
    }

    import DateTime.now
    val creationDate = now.minusDays(3)
    val expirationDate = now.minusDays(1)
    val lastUsed = now.minusDays(2)
    val withExpires = authenticator.copy(creationDate = creationDate,
      expirationDate = expirationDate,
      lastUsed = lastUsed)

    Logger.debug(s"Authenticator id: $withExpires.id")
    //Note: SecureSocial needs to look up the authenticator from the cache
    import play.api.Play.current
    Cache.set(withExpires.id,withExpires)
    withExpires.toCookie
  }

  def tokenFormBody(id: String, secret: String, username: String, grantType: Option[String] = None): Array[(String, String)] = {
    val signature = ShaHash.sign(
      OAuthConstants.ClientCredentials+":"+id+":"+OAuthConstants.Sha1Hash+":"+username,
      secret
    )
    val base = Array(
      (OAuthConstants.ClientId -> id),
      (OAuthConstants.ClientSecret -> secret),
      (OAuthConstants.Scope -> username))
    base ++ grantType.map((OAuthConstants.GrantType -> _))
  }


}
