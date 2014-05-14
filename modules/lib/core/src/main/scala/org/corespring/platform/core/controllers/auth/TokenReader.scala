package org.corespring.platform.core.controllers.auth

import play.api.mvc.RequestHeader

trait TokenReader{

  val AuthorizationHeader = "Authorization"
  val AccessToken = "access_token"
  val Bearer = "Bearer"
  val Space = " "

  def getToken[E](request:RequestHeader, invalidToken:E, noToken:E) : Either[E,String] = {

    def tokenInHeader : Option[Either[E,String]] = {
      request.headers.get(AuthorizationHeader).map{ h =>
        h.split(Space) match {
          case Array(Bearer, token) => Right(token)
          case _ => Left(invalidToken)
        }
      }.flatten
    }

    Left(invalidToken)

    //try query string
    //then try session
    //then try headers

    val queryToken : Unit => Option[String] =  _ => request.queryString.get(AccessToken).map(_.head)
    val sessionToken : Unit => Option[String] =  _ => request.session.get(AccessToken)
    val headerToken : Unit => Option[Either[E,String]] = _ =>  tokenInHeader

    /*Seq(

    )

    val out : Either[E,String]] = for{
      query <- request.queryString.get(AccessToken).map(Right(_.head))
    }.map(_.head)
      .orElse(request.session.get(AccessToken))
      .orElse(tokenInHeader)

    out
    */
  }
}
