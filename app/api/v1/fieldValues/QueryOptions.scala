package api.v1.fieldValues

import play.api.libs.json._


case class Options(query:Option[String],filter:Option[String], skip : Int, limit : Int)


object QueryOptions {

  val DEFAULT_SKIP = 0
  val DEFAULT_LIMIT = 50
  val EmptyOptions = Options(None,None, DEFAULT_SKIP, DEFAULT_LIMIT)

  /**
   * Extract json to an Options model, looks for:
   * q : JsObject -> query
   * f : JsObject -> limit
   * sk : JsNumber -> skip
   * l : JsNumber -> limit
   */
  def unapply(json:JsValue) : Option[Options] = json match {
    case JsObject(list) => {
      val query : Option[String] = getOpt("q", list)
      val filter : Option[String] = getOpt("f", list)
      val skip : Int = list.find(_._1 == "sk").map( (t:(String,JsValue))=> t._2.as[Int]).getOrElse(DEFAULT_SKIP)
      val limit : Int = list.find(_._1 == "l").map( (t:(String,JsValue))=> t._2.as[Int]).getOrElse(DEFAULT_LIMIT)
      Some(Options(query,filter,skip,limit))
    }
    case _ => None 
  }

  private def getOpt(key:String,l:Seq[(String,JsValue)]) = l.find(_._1 == key).map((t:(String,JsValue))=> Json.stringify(t._2))

}