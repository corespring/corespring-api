package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.auth.Permission
import org.corespring.models.item.{PlayerDefinition, Item, Passage}
import org.corespring.models.item.resource.BaseFile
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrgCollectionService
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{PlayerAccessSettings, OrgAndOpts}
import org.corespring.v2.errors.Errors.orgCantAccessCollection
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration.Duration
import scalaz.{Failure, Success}

class PassageAccessTest extends Specification with Mockito {

  "grant" should {

    trait PassageAccessScope extends Scope {
      val orgCollectionService = mock[OrgCollectionService]
      val itemService = mock[ItemService]
      val passageAccess = new PassageAccess(orgCollectionService, itemService)

      val organization = Organization(id = new ObjectId(), name = "")
      val identity = OrgAndOpts(org = organization, opts = mock[PlayerAccessSettings], authMode = mock[AuthMode],
        apiClientId = None)

      val passage = Passage(collectionId = new ObjectId().toString, file = mock[BaseFile])
      val itemId = VersionedId[ObjectId](new ObjectId(), Some(0))

      val executionContext = ExecutionContext.global

      val itemWithPassage = Item(collectionId = new ObjectId().toString,
        playerDefinition = Some(PlayerDefinition("", Json.obj(
          "1" -> Json.obj(
            "componentType" -> "corespring-passage",
            "id" -> passage.id.toString
          )
        ))))

      val itemWithoutPassage = Item(collectionId = new ObjectId().toString)
    }

    "org can access passage collection" should {

      trait OrgCanAccessScope extends PassageAccessScope {
        orgCollectionService.isAuthorized(organization.id, new ObjectId(passage.collectionId), Permission.Read) returns true
      }

      "grant access" in new OrgCanAccessScope {
        Await.result(passageAccess.grant(identity, Permission.Read, (passage, None)), Duration.Inf) must be equalTo(
          Success(true))
      }

    }

    "org cannot access passage collection" should {

      trait OrgCannotAccessPassage extends PassageAccessScope {
        orgCollectionService.isAuthorized(organization.id, new ObjectId(passage.collectionId), Permission.Read) returns false
      }

      "deny access" in new OrgCannotAccessPassage {
        Await.result(passageAccess.grant(identity, Permission.Read, (passage, None)), Duration.Inf) must be equalTo(
          Failure(orgCantAccessCollection(identity.org.id, passage.collectionId, Permission.Read.name)))
      }

      "item id contains passage" should {

        trait ItemWithPassage extends OrgCannotAccessPassage {
          orgCollectionService.isAuthorized(organization.id, new ObjectId(itemWithPassage.collectionId), Permission.Read) returns true
          itemService.findOneById(itemId) returns Some(itemWithPassage)
        }

        "grant access" in new ItemWithPassage {
          Await.result(passageAccess.grant(identity, Permission.Read, (passage, Some(itemId))), Duration.Inf) must be equalTo(Success(true))
        }

      }

      "item id does not contain passage" should {

        trait ItemWithoutPassage extends OrgCannotAccessPassage {
          orgCollectionService.isAuthorized(organization.id, new ObjectId(itemWithoutPassage.collectionId), Permission.Read) returns true
          itemService.findOneById(itemId) returns Some(itemWithoutPassage)
        }

        "deny access" in new ItemWithoutPassage {
          Await.result(passageAccess.grant(identity, Permission.Read, (passage, Some(itemId))), Duration.Inf) must be equalTo(
            Failure(orgCantAccessCollection(identity.org.id, passage.collectionId, Permission.Read.name)))
        }

      }

    }

  }

}
