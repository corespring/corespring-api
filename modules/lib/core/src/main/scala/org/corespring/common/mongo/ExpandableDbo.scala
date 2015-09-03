package org.corespring.common.mongo

import com.mongodb.casbah.Imports._
import play.api.Logger

object ExpandableDbo {

  lazy val logger = Logger(classOf[ExpandableDbo])

  implicit class ExpandableDbo(dbo: DBObject) {

    def expandPath(path: String): Option[DBObject] = {

      logger.debug(s"[expandPath] $path, $dbo")

      val parts = path.split("\\.").toList
      val out = expand(parts, dbo)
      logger.trace(s"[expandPath] return: $out")
      out
    }

    private def toIndex(s: String): Option[Int] = try {
      Some(s.toInt)
    } catch {
      case t: Throwable => None
    }

    private def fromList(l: MongoDBList, part: Option[String]): Option[DBObject] = {
      part
        .flatMap(toIndex)
        .flatMap { i: Int =>
          try {
            l.get(i) match {
              case null => None
              case o: DBObject => Some(o)
              case l: MongoDBList => Some(l)
              case _ => None
            }
          } catch {
            case t: Throwable => None
          }
        }
    }

    private def expand(parts: Seq[String], acc: DBObject): Option[DBObject] = {
      parts match {
        case Nil => Some(acc)
        case head :: Nil => acc.getAs[DBObject](head)
        case head :: xs => {
          val inner: Any = acc.get(head)
          inner match {
            case null => None
            case o: DBObject => expand(xs, o)
            case l: MongoDBList => {
              val child = fromList(l, xs.headOption)
              child.flatMap { c =>
                expand(xs.drop(1), c)
              }
            }
          }
        }
        case _ => None
      }
    }
  }
}
