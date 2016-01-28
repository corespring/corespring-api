package org.corespring.services.salat.auth

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatSaveError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }

import scalaz.{ Failure, Success, Validation }

class ApiClientService(orgService: interface.OrganizationService,
  val dao: SalatDAO[ApiClient, ObjectId],
  val context: Context) extends interface.auth.ApiClientService with HasDao[ApiClient, ObjectId] {

  private val logger = Logger[ApiClientService]()

  object Keys {
    val clientId = "clientId"
    val clientSecret = "clientSecret"
    val orgId = "orgId"
  }

  /**
   * Retrieves an ApiClient by client id and secret from the services.
   * @param id - the client id
   * @param secret - the client secret
   * @return an Option[ApiClient]
   */
  def findByClientIdAndSecret(id: String, secret: String): Option[ApiClient] = {
    val idsObj = MongoDBObject(Keys.clientId -> new ObjectId(id), Keys.clientSecret -> secret)
    dao.findOne(idsObj)
  }

  def findByClientId(id: String): Option[ApiClient] = {
    logger.trace(s"api client count:  ${dao.count()}")
    dao.findOne(MongoDBObject(Keys.clientId -> new ObjectId(id)))
  }

  def findByOrgId(orgId: ObjectId): Stream[ApiClient] = dao.find(MongoDBObject(Keys.orgId -> orgId)).toStream

  /**
   * Generates a random token
   *
   * @return a token
   */
  private def generateTokenId(): String = {
    ObjectId.get.toString
  }

  /**
   * Creates an ApiClient for an organization.  This allows organizations to receive API calls
   *
   * @param orgId - the organization id
   * @return returns an ApiClient or ApiError if the ApiClient could not be created.
   */
  override def getOrCreateForOrg(orgId: ObjectId): Validation[String, ApiClient] = {
    findByOrgId(orgId).headOption match {
      case Some(apiClient) => Success(apiClient)
      case None => {
        // check we got an existing org id
        orgService.findOneById(orgId) match {
          case Some(org) =>
            val apiClient = ApiClient(orgId, new ObjectId(), generateTokenId())
            try {
              dao.save(apiClient)
              Success(apiClient)
            } catch {
              case e: SalatSaveError => {
                logger.error("Error registering organization %s".format(orgId), e)
                val OperationError = "There was an error processing your request"
                Failure(OperationError)
              }
            }
          case None => Failure(s"No organization found with id: $orgId")
        }
      }
    }
  }
}
