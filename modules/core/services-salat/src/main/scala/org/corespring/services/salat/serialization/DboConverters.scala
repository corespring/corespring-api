package org.corespring.services.salat.serialization

import com.mongodb.casbah.Implicits.wrapDBObj
import com.mongodb.casbah.Imports._
import com.novus.salat.json.JSONConfig
import java.text.DateFormat
import java.util.Date
import org.bson.types.BSONTimestamp
import org.joda.time.{ DateTimeZone, DateTime }
import org.joda.time.format.DateTimeFormatter
import play.api.data.validation.ValidationError
import play.api.libs.json._

/**
 * Lifted from salat but using play-json's JsValue instead of json4s's JValue
 */
object ToJsValue {

  def apply(o: Any): JsValue = o.asInstanceOf[AnyRef] match {
    case t: MongoDBList => JsArray(t.map(apply).toList)
    case t: BasicDBList => JsArray(t.map(apply).toList)
    case dbo: DBObject => JsObject(wrapDBObj(dbo).toList.map(v => v._1 -> apply(v._2)))
    case ba: Array[Byte] => JsArray(ba.toList.map(JsNumber(_)))
    case m: Map[_, _] => JsObject(m.toList.map(v => v._1.toString -> apply(v._2)))
    case m: java.util.Map[_, _] => JsObject(scala.collection.JavaConversions.mapAsScalaMap(m).toList.map(v => v._1.toString -> apply(v._2)))
    case iter: Iterable[_] => JsArray(iter.map(apply).toList)
    case iter: java.lang.Iterable[_] => JsArray(scala.collection.JavaConversions.iterableAsScalaIterable(iter).map(apply).toList)
    case x => serialize(x)
  }

  val dateFormatter: DateTimeFormatter = JSONConfig.ISO8601

  def serialize(o: Any): JsValue = {
    val v = o match {
      case s: String => JsString(s)
      case c: Char => JsString(c.toString)
      case d: Double => if (d.isNaN || d.isInfinite) JsNull else JsNumber(d) // Double.NaN is invalid JSON
      case f: Float => JsNumber(f.toDouble)
      case s: Short => JsNumber(s.toDouble)
      case bd: BigDecimal => JsNumber(bd.toDouble)
      case i: Int => JsNumber(i)
      case bi: BigInt => JsNumber(bi.toDouble)
      case l: Long => JsNumber(l)
      case b: Boolean => JsBoolean(b)
      case d: java.util.Date => JsString(dateFormatter.print(d.getTime))
      case d: DateTime => JsString(dateFormatter.print(d))
      case tz: java.util.TimeZone => JsString(tz.getID)
      case tz: DateTimeZone => JsString(tz.getID)
      case o: ObjectId => JsString(o.toString)
      case u: java.net.URL => JsString(u.toString) // might as well
      case n if n == null => JsNull
      case ts: BSONTimestamp => sys.error("BSONTimestamp - in: unexpected OID input class='%s', value='%s'".format(ts.getClass.getName, ts))
      case x: AnyRef => sys.error("serialize: Unsupported JSON transformation for class='%s', value='%s'".format(x.getClass.getName, x))
    }
    v
  }
}

/**
 * Lifted from:  https://gist.github.com/doitian/5555040
 */
object ToDBObject {

  def apply(json: JsValue): DBObject = {
    import com.mongodb.casbah.Imports._
    fromJson(json).asOpt.getOrElse(MongoDBObject())
  }

  def fromJson(json: JsValue): JsResult[DBObject] = readDBObject.reads(json)

  implicit val readDBObject = new Reads[DBObject] {
    def reads(js: JsValue): JsResult[DBObject] = {
      parsePlainObject(js.asInstanceOf[JsObject], JsPath())
    }

    private def parsePlainObject(obj: JsObject, parent: JsPath): JsResult[DBObject] = {
      parsePlainFields(obj.fields.toList, parent).map(DBObject(_))
    }

    private def parsePlainFields(l: List[(String, JsValue)], parent: JsPath): JsResult[List[(String, Any)]] = {
      l match {
        case Nil => JsSuccess(Nil, parent)
        case head :: tail => cons(
          parse(head._2, (parent \ head._1)).map(head._1 -> _),
          parsePlainFields(tail, parent))
      }
    }

    private def parse(obj: JsObject, parent: JsPath): JsResult[Any] = {
      if (obj.fields.length > 0) {
        obj.fields(0) match {
          case ("$date", v: JsValue) =>
            val path = parent \ "$date"
            try {
              v match {
                case number: JsNumber => JsSuccess(new Date(number.value.toLong), path)
                case _ => JsSuccess(DateFormat.getDateInstance().parse(v.toString))
              }
            } catch {
              case ex: IllegalArgumentException => JsError(path, ValidationError("validation.invalid", "$date"))
            }
          case ("$oid", v: JsString) =>
            val path = parent \ "$oid"
            try {
              JsSuccess(new ObjectId(v.value), path)
            } catch {
              case ex: IllegalArgumentException => JsError(path, ValidationError("validation.invalid", "$oid"))
            }
          case _ => parsePlainObject(obj, parent)
        }
      } else parsePlainObject(obj, parent)
    }

    private def parse(arr: JsArray, parent: JsPath): JsResult[List[Any]] = {
      parse(arr.value.toList, parent, 0)
    }

    private def parse(l: List[JsValue], parent: JsPath, i: Int): JsResult[List[Any]] = {
      l match {
        case Nil => JsSuccess(Nil)
        case head :: tail => cons(parse(head, parent(i)), parse(tail, parent, i + 1))
      }
    }

    private def cons[T](head: JsResult[T], tail: JsResult[List[T]]): JsResult[List[T]] = {
      (head, tail) match {
        case (h: JsError, t: JsError) => h ++ t
        case (JsSuccess(h, _), JsSuccess(t, _)) => JsSuccess(h :: t)
        case (h: JsError, _) => h
        case _ => tail
      }
    }

    private def parse(js: JsValue, parent: JsPath): JsResult[Any] = {
      js match {
        case v: JsObject => parse(v, parent)
        case v: JsArray => parse(v, parent)
        case v: JsString => JsSuccess(v.value, parent)
        case v: JsNumber => JsSuccess(v.value.toDouble, parent)
        case v: JsBoolean => JsSuccess(v.value, parent)
        case JsNull => JsSuccess(null)
        case _: JsUndefined => JsSuccess(null)
      }
    }
  }
}

