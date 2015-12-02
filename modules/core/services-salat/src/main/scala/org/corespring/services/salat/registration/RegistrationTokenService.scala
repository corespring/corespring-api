package org.corespring.services.salat.registration

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.models.registration.RegistrationToken
import org.corespring.{services => interface}
import org.joda.time.DateTime

class RegistrationTokenService (dao:SalatDAO[RegistrationToken, ObjectId], context:Context)
  extends interface.RegistrationTokenService{

  override def createToken(token: RegistrationToken): Boolean = {
    dao.insert(token)
    true
  }

  override def findTokenByUuid(uuid: String): Option[RegistrationToken] = dao.findOne(MongoDBObject(RegistrationToken.Keys.Uuid -> uuid))

  override def deleteToken(uuid: String): Boolean = {
    dao.remove(MongoDBObject(RegistrationToken.Keys.Uuid -> uuid))
    true
  }

  override def deleteExpiredTokens(): Int = {
    val currentTime = new DateTime()
    val r = dao.remove(MongoDBObject(RegistrationToken.Keys.Expires -> MongoDBObject("$lt" -> currentTime)))
    r.getN
  }
}
