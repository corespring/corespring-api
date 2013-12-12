package org.corespring.qti.models.responses.javascript

import org.corespring.qti.models.responses._
import play.api.libs.json.JsValue
import org.corespring.qti.models.interactions._
import org.corespring.qti.models.responses.Response.ResponseWrites._
import com.scalapeno.rhinos.JavascriptProcessor

trait DefaultResponseConverter extends Interaction with JavascriptProcessor {

  val stringResponseConverter: String =
    """
      var returnValue = {
        value: response['value']
      };
      returnValue;
    """

  val arrayResponseConverter: String =
    """
      var returnValue = { value: [] };

      for (var i = 0; i < response['value'].length; i++) {
        returnValue['value'].push(response['value'][i]);
      }
      returnValue;
    """

  abstract override def toJs(response: Response): Option[(String,JsValue)] = {
    response match {
      case stringResponse: StringResponse => js(stringResponseConverter, Map("response" -> writes(stringResponse)))
      case arrayResponse: ArrayResponse => js(arrayResponseConverter, Map("response" -> writes(arrayResponse)))
      case _ => None
    }
  }

}
