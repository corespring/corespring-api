package org.corespring.v2

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.errors._
import org.corespring.v2.errors.Errors._
import play.api.libs.json.Json
import play.api.test.FakeRequest

/**
 * A simple App that prints examples of all error classes in a JSON markdown format.
 */
object Exporter extends App {

  val fakeGet = FakeRequest("GET", "/path?access_token=access_token")

  val examples: Seq[V2Error] = Seq(
    invalidObjectId("id", "context"),
    missingRequiredField(Field("name", "type")),
    encryptionFailed("msg"),
    permissionNotGranted("msg"),
    compoundError("msg", Seq(), 401),
    invalidQueryStringParameter("badName", "expectedName"),
    noApiClientAndPlayerTokenInQueryString(FakeRequest()),
    noToken(fakeGet),
    noUserSession(fakeGet),
    invalidToken(fakeGet),
    expiredToken(fakeGet),
    noOrgForToken(fakeGet),
    noDefaultCollection(new ObjectId()),
    generalError("msg"),
    noJson,
    invalidJson("str"),
    errorSaving,
    needJsonHeader,
    propertyNotFoundInJson("name"),
    noOrgIdAndOptions(fakeGet),
    noCollectionIdForItem(new VersionedId(new ObjectId(), Some(0))),
    invalidCollectionId("collectionId", new VersionedId(new ObjectId(), Some(0))),
    orgCantAccessCollection(new ObjectId(), "collectionId", "read"),
    cantLoadSession("sessionId"),
    noItemIdInSession("sessionId"),
    cantParseItemId("itemId"),
    cantFindItemWithId(new VersionedId(new ObjectId(), Some(0))),
    cantFindOrgWithId(new ObjectId()),
    addAnswerRequiresId(new ObjectId()),
    aggregateRequiresItemId(new ObjectId()),
    invalidPval(3L, "collectionId", new ObjectId()),
    incorrectJsonFormat(Json.obj()),
    orgDoesntReferToCollection(new ObjectId(), "collectionId"),
    inaccessibleItem(new VersionedId(new ObjectId(), Some(0)), new ObjectId(), Permission.Write),
    unAuthorized("error", "error"),
    insufficientPermission(1L, Permission.Write),
    cantFindSession("sessionId"),
    sessionDoesNotContainResponses("sessionId"))

  examples.foreach(error => {
    println(s"""
      |### ${error.title}
      |${error.description}
      |
      |```
      |${Json.prettyPrint(error.json)}
      |```
      |""".stripMargin)
  })

}
