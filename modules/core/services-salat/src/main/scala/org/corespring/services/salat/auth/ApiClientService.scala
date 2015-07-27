package org.corespring.services.salat.auth

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.{SalatSaveError, SalatDAO}
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient
import org.corespring.services.salat.{HasDao}
import org.corespring.{ services => interface }

class ApiClientService( orgService : interface.OrganizationService,
                        val dao : SalatDAO[ApiClient,ObjectId],
                        val context : Context
                        ) extends interface.auth.ApiClientService with HasDao[ApiClient, ObjectId] {

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
  def findByIdAndSecret(id: String, secret: String): Option[ApiClient] = {
    val idsObj = MongoDBObject(Keys.clientId -> new ObjectId(id), Keys.clientSecret -> secret)
    dao.findOne(idsObj)
  }

  def findByKey(key: String): Option[ApiClient] = {
    logger.trace(s"api client count:  ${dao.count()}")
    dao.findOne(MongoDBObject(Keys.clientId -> new ObjectId(key)))
  }

  def findOneByOrgId(orgId: ObjectId): Option[ApiClient] = dao.findOne(MongoDBObject(Keys.orgId -> orgId))

  private def KEY_LENGTH = 16

  private def KEY_RADIX = 36
  /**
   * Generates a random token
   *
   * @return a token
   */
  private def generateToken(keyLength: Int = KEY_LENGTH) = {
    BigInt.probablePrime(keyLength * 8, scala.util.Random).toString(KEY_RADIX)
  }

  /**
   * Creates an ApiClient for an organization.  This allows organizations to receive API calls
   *
   * @param orgId - the organization id
   * @return returns an ApiClient or ApiError if the ApiClient could not be created.
   */
  override def createForOrg(orgId: ObjectId): Either[String, ApiClient] = {
      findOneByOrgId(orgId) match {
        case Some(apiClient) => Right(apiClient)
        case None => {
          // check we got an existing org id
          orgService.findOneById(orgId) match {
            case Some(org) =>
              val apiClient = ApiClient(orgId, new ObjectId(), generateToken())
              try {
                dao.save(apiClient)
                Right(apiClient)
              } catch {
                case e: SalatSaveError => {
                  logger.error("Error registering ortganization %s".format(orgId), e)
                  val OperationError = "There was an error processing your request"
                  Left(OperationError)
                }
              }
            case None => Left(s"No organization found with id: $orgId")
          }
        }
      }
    }
}
