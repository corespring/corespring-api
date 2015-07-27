package org.corespring.services.salat.registration

import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.models.registration.RegistrationToken
import org.corespring.{services => interface}
class RegistrationTokenService (dao:SalatDAO[RegistrationToken, ObjectId], context:Context)
  extends interface.RegistrationTokenService{

  override def createToken(token: RegistrationToken): Boolean = ???

  override def findTokenByUuid(uuid: String): Option[RegistrationToken] = ???

  override def deleteTokenUuid(uuid: String): Boolean = ???

  override def deleteExpiredTokens(): Int = ???
}
