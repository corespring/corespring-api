package org.corespring.models.json.item

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import org.corespring.models.item.{ StandardCluster, Subjects, TaskInfo }
import org.corespring.models.json.ValueGetter
import play.api.data.validation.ValidationError
import play.api.libs.json._

trait TaskInfoFormat extends ValueGetter with Format[TaskInfo] {

  object Keys {
    val description = "description"
    val domains = "domains"
    val extended = "extended"
    val gradeLevel = "gradeLevel"
    val itemType = "itemType"
    val standardClusters = "standardClusters"
    val subjects = "subjects"
    val title = "title"
    val originId = "originId"
  }

  implicit def sf: Format[Subjects]
  implicit def scf: Format[StandardCluster]

  override def writes(info: TaskInfo): JsValue = {

    import Keys._

    val infoJson = JsObject(Seq(
      if (info.gradeLevel.isEmpty) None else Some((gradeLevel -> JsArray(info.gradeLevel.map(JsString(_))))),
      info.title.map((title -> JsString(_))),
      info.description.map((description -> JsString(_))),
      info.itemType.map((itemType -> JsString(_))),
      Some(domains -> JsArray(info.domains.toSeq.map(d => JsString(d)))),
      info.originId.map((originId -> JsString(_))),
      Some(standardClusters -> JsArray(info.standardClusters.map(Json.toJson(_)))),
      if (info.extended.isEmpty) None else Some((extended -> extendedAsJson(info.extended)))).flatten)

    val subjectsJson: Option[JsValue] = info.subjects.map(subjects => Json.toJson(subjects))

    subjectsJson match {
      case Some(js) => {
        val jsObjects = Seq(infoJson, js).filter(_.isInstanceOf[JsObject]).map(_.asInstanceOf[JsObject])
        jsObjects.tail.foldRight(jsObjects.head)(_ ++ _)
      }
      case _ => infoJson
    }
  }

  def extendedAsJson(extended: Map[String, DBObject]): JsValue = {
    JsObject(extended.foldRight[Seq[(String, JsValue)]](Seq())((md, acc1) => {
      acc1 :+ (md._1 -> JsObject(md._2.toSeq.map(prop => prop._1 -> JsString(prop._2.toString))))
    }))
  }

  override def reads(json: JsValue): JsResult[TaskInfo] = {

    val info: TaskInfo = TaskInfo(
      itemType = (json \ Keys.itemType).asOpt[String],
      title = (json \ Keys.title).asOpt[String],
      description = (json \ Keys.description).asOpt[String],
      extended = getExtended(json),
      subjects = getSubjects(json),
      domains = getDomains(json),
      originId = (json \ Keys.originId).asOpt[String],
      standardClusters = getStandardClusters(json),
      gradeLevel = getGradeLevel(json))
    JsSuccess(info)
  }

  private def getSubjects(json: JsValue): Option[Subjects] = {
    Json.fromJson[Subjects](json) match {
      case JsSuccess(s, _) => Some(s)
      case _ => None
    }
  }

  private def getDomains(json: JsValue): Set[String] = {
    (json \ Keys.domains).asOpt[Seq[String]] match {
      case Some(domains) => domains.toSet
      case None => Set.empty
    }
  }

  private def getStandardClusters(json: JsValue): Seq[StandardCluster] = {
    (json \ Keys.standardClusters).asOpt[Seq[StandardCluster]] match {
      case Some(clusters) => clusters
      case None => Seq.empty
    }
  }

  private def getExtended(json: JsValue): Map[String, Imports.BasicDBObject] = {

    def toDbo(json: JsValue): BasicDBObject = json match {
      case obj: JsObject => com.mongodb.util.JSON.parse(Json.stringify(obj)).asInstanceOf[BasicDBObject]
      case _ => throw new RuntimeException("only objects supported")
    }

    def toKeyAndDbo(t: (String, JsValue)): (String, BasicDBObject) = {
      val (k, v) = t
      k -> toDbo(v)
    }

    (json \ Keys.extended).asOpt[JsObject].map { o =>
      o.fields.map(toKeyAndDbo).toMap
    }.getOrElse(Map())
  }

  private def isValid(g: String) = fieldValues.gradeLevels.exists(_.key == g)

  private def getGradeLevel(json: JsValue) = {
    (json \ Keys.gradeLevel).asOpt[Seq[String]] match {
      case Some(grades) => if (grades.forall(isValid(_))) grades else Seq.empty
      case None => Seq.empty
    }
  }
}

