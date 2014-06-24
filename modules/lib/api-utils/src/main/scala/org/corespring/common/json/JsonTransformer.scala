package org.corespring.common.json

import play.api.libs.json._

/**
 * Define a transformation from one type of json to another
 *
 * @param mapArray the json mapping definition.
 *                 When running transform - the left string is the source key, the right the target.
 *                 When running reverseTransform its the opposite.
 *                 Eg:
 *                 {{{
 *                     val t = new JsonTransform(
 *                       "apple.name" -> "myApple.myName",
 *                       "orange.flavour" -> "myOrangeFlavour")
 *                 }}}
 *
 */
abstract class JsonTransformer(mapArray: (String, String)*) {

  require(mapArray.length > 0)

  private lazy val keysReversed: Seq[(String, String)] = mapArray.map(tuple => tuple._2 -> tuple._1)

  def transform(implicit in: JsValue): JsValue = applyMapping(mapArray)

  def reverseTransform(implicit in: JsValue): JsValue = applyMapping(keysReversed)

  private def applyMapping(m: Seq[(String, String)])(implicit in: JsValue): JsValue = {

    def pruneKeys: Seq[String] = {
      m.map(_._1.split("\\.")(0))
    }

    val out = m.map(tuple => copy(tuple._1, tuple._2))
    val pruned = prune(pruneKeys: _*)
    merge((out :+ pruned): _*)
  }

  private def copy(fromTo: (String, String))(implicit in: JsValue) = {
    val (from, to) = fromTo
    def foldFn(p: JsPath, s: String) = (p \ s)
    val fromPath: JsPath = from.split("\\.").foldLeft[JsPath](__)(foldFn)
    val toPath: JsPath = to.split("\\.").foldLeft[JsPath](__)(foldFn)

    val validation = toPath.json.copyFrom(fromPath.json.pick)
    val out = in.validate(validation).asOpt
    out
  }

  private def prune(nodes: String*)(implicit in: JsValue) = {
    val paths: Seq[Reads[JsObject]] = nodes.map(n => (__ \ n).json.prune)
    val cmd = paths.foldRight[Reads[JsObject]](new EmptyReads) { (n: Reads[JsObject], r: Reads[JsObject]) => r andThen n }
    in.validate(cmd).asOpt
  }

  class EmptyReads extends Reads[JsObject] {

    def reads(json: JsValue): JsResult[JsObject] = {
      if (json.isInstanceOf[JsObject]) {
        JsSuccess(json.asInstanceOf[JsObject])
      } else
        JsError("?")
    }
  }

  private def merge(results: Option[JsObject]*): JsObject = {
    val success: Seq[JsObject] = results.toSeq.flatten
    success.fold[JsObject](JsObject(Seq())) { (acc: JsObject, i: JsObject) =>

      println(s"merge: $acc + with $i")
      acc.deepMerge(i)
      //(__).json.update()
      //acc ++ i
    }
  }
}
