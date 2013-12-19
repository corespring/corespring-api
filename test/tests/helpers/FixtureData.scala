package tests.helpers

import org.specs2.mutable.BeforeAfter
import tests.helpers.models._

/*
 * Including this trait in the context of a mutable.Specification "in" block will make available all the fixture data.
 * After the test has run, the FixtureData.after method is called to tear down the data. Example:
 *
 *   class UserNameTest extends Specification {
 *
 *     "user has name" in FixtureData {
 *       user.userName !== ""
 *     }
 *
 *   }
 *
 */
trait FixtureData extends BeforeAfter {

  /*
   * TODO: Obviously these are all evaluated by the after method... need to figure out how to not instantiate them if we
   * don't need to.
   */
  lazy val organizationId = OrganizationHelper.create()
  lazy val collectionId = CollectionHelper.create(organizationId)
  lazy val user = UserHelper.create(organizationId)
  lazy val accessToken = AccessTokenHelper.create(organizationId, user.userName)

  val collectionItemsCount = 3
  val itemIds = 1.to(collectionItemsCount).map(i => ItemHelper.create(collectionId))

  def before : Unit = {
    println(s"[FixtureData] org: $organizationId, collection: $collectionId, user: ${user.id}, token: $accessToken")
  }

  def after : Unit = {
    println(s"[FixtureData] deleting: org: $organizationId, collection: $collectionId, user: ${user.id}, token: $accessToken")
    OrganizationHelper.delete(organizationId)
    CollectionHelper.delete(collectionId)
    UserHelper.delete(user.id)
    AccessTokenHelper.delete(accessToken)
    itemIds.foreach(ItemHelper.delete(_))
  }

}