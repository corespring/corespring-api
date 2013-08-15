package models.metadata

import models.{Metadata, MetadataSet, SchemaMetadata}
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import tests.PlaySingleton

class SetJsonTest extends Specification {


  PlaySingleton.start()

  "SetJson" should {

    def toJsonString(p:(String,String)) = s""" "${p._1}" : "${p._2}" """

    def dataArray( m : Metadata) : String = m.properties.map(toJsonString).mkString(",")

    def mkJson(s:MetadataSet, m : Option[Metadata]) : String = {


      val string = s"""
          {
            "id" : "${s.id}",
            "metadataKey" : "${s.metadataKey}",
            "editorLabel" : "${s.editorLabel}",
            "editorUrl" : "${s.editorUrl}",
            "isPublic" : ${s.isPublic},
            "schema" : [ ${s.schema.map( sc =>  s""" {"key" : "${sc.key}"} """).mkString(",")}]
            ${ if( m.isDefined) s""", "data" : { ${dataArray(m.get)}}""" else ""}
          }
        """

      println(string)
      string
    }


    def assert(set:MetadataSet, m : Option[Metadata]) = {
      val ref = SetJson(set,m)

      println(s"Generated json: ${Json.stringify(ref)}")

      JsonCompare.caseInsensitiveSubTree(Json.stringify(ref), mkJson(set,m)) match {
        case Left(diffs) => {
          println(diffs)
          failure(diffs.mkString(","))
        }
        case Right(_) => success
      }
    }

    "create some json" in assert(
      MetadataSet( "one", "url", "label", true, Seq(SchemaMetadata("apple"))),
      Some(Metadata("one", Map("apple" -> "Granny Smith")))
    )

    "create some json" in assert(
      MetadataSet( "one", "url", "label", true, Seq(SchemaMetadata("apple"))),
      None
    )

  }
}
