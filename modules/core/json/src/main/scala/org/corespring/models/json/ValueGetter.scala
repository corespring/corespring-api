package org.corespring.models.json

import org.corespring.models.item.FieldValue
import play.api.libs.json.JsValue

trait ValueGetter {

  /**
   * Get a sequence of values from json and pass them to the makeObject function
   * @param json
   * @param names
   * @param makeObject
   * @tparam A
   * @return
   */
  def get[A](json: JsValue, names: Seq[String], makeObject: (Seq[Option[String]] => Option[A])): Option[A] = {

    /**
     * Get the values from the json
     * eg: subject,othersubject -> Math,English
     */
    val vals: Seq[Option[String]] = names.map((n: String) => {
      (json \ n).asOpt[String]
    })

    makeObject(vals)
  }

  def fieldValues: FieldValue
}
