package models.web

import controllers.web.services.UserService
import controllers.web.routes

case class User(username:String,password:String)

object User{

  def authenticate(email:String, password: String ) : Option[User] = {
    UserService.login(email, password)
  }
  def findByEmail(email:String):Option[User] = {
    Some(User("ed", "password"))
  }
}
