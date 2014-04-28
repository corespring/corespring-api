package org.corespring.test.helpers

import org.corespring.common.log.PackageLogging
import org.corespring.test.helpers.models._
import org.specs2.mutable.BeforeAfter

/**
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
trait FixtureData extends BeforeAfter with PackageLogging{

  override def loggerName = "org.corespring.test.helpers.FixtureData"
  lazy val organizationId = OrganizationHelper.create()
  lazy val collectionId = CollectionHelper.create(organizationId)
  lazy val user = UserHelper.create(organizationId)
  lazy val accessToken = AccessTokenHelper.create(organizationId, user.userName)

  val collectionItemsCount = 3
  val itemIds = 1.to(collectionItemsCount).map(i => ItemHelper.create(collectionId))

  def before : Unit = {
    logger.debug(s"org: $organizationId, collection: $collectionId, user: ${user.id}, token: $accessToken")
  }

  def after : Unit = {
    logger.debug(s"deleting: org: $organizationId, collection: $collectionId, user: ${user.id}, token: $accessToken")
    OrganizationHelper.delete(organizationId)
    CollectionHelper.delete(collectionId)
    UserHelper.delete(user.id)
    AccessTokenHelper.delete(accessToken)
    itemIds.foreach(ItemHelper.delete(_))
  }

}