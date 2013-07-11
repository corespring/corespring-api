package common.models

import org.bson.types.ObjectId
import org.codehaus.jackson.map.module.SimpleModule
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.Json._
import play.api.libs.json._


package object json {

  implicit object ObjectIdReads extends Reads[ObjectId] {
    def reads(js: JsValue): ObjectId = {
      if (ObjectId.isValid(js.as[String]))
        new ObjectId(js.as[String])
      else
        throw new RuntimeException("Invalid object id")
    }
  }

  implicit object ObjectIdWrites extends Writes[ObjectId] {
    def writes(oid: ObjectId): JsValue = JsObject(Seq("$oid" -> JsString(oid.toString)))
  }

  /**
   * Convenience Reads/Writes that use jerkson instead of having to manually serialize the data.
   */
  object jerkson {

    trait JerksonWrites[A] extends Writes[A] {
      def writes(c: A) = {
        println("Writes ---> " + c)
        val json = CorespringJson.generate(c)
        println("Writes ---> " + json)
        play.api.libs.json.Json.parse(json)
      }
    }

    @deprecated("will be removed once we move to 2.1.1 and the Format macro it provides", "2.1.1")
    trait JerksonReads[A] extends Reads[A] {

      //TODO: Any way we can avoid having to do this?
      def manifest: Manifest[A]

      def reads(js: JsValue): A = {
        CorespringJson.parse[A](stringify(js))(manifest)
      }
    }

    import org.bson.types.ObjectId
import org.codehaus.jackson.map.annotate.JsonCachable
import org.codehaus.jackson.map.{DeserializationContext, JsonDeserializer, SerializerProvider, JsonSerializer}
import org.codehaus.jackson.{Version, JsonParser, JsonGenerator}

    @JsonCachable
    class ObjectIdSerializer extends JsonSerializer[ObjectId] {
      def serialize(id: ObjectId, json: JsonGenerator, provider: SerializerProvider) {
        json.writeString(id.toString)
      }
    }

    class ObjectIdDeserializer extends JsonDeserializer[ObjectId] {
      def deserialize(jp: JsonParser, context: DeserializationContext) = {
        if (!ObjectId.isValid(jp.getText)) throw context.mappingException("invalid ObjectId " + jp.getText)
        new ObjectId(jp.getText)
      }
    }

    @JsonCachable
    class VersionedIdSerializer extends JsonSerializer[VersionedId[_]] {
      def serialize(id: VersionedId[_], json: JsonGenerator, provider: SerializerProvider) {
        import models.versioning.VersionedIdImplicits.Binders._
println("versioned id writes ... > " + id)
        json.writeString(versionedIdToString(id.asInstanceOf[VersionedId[ObjectId]]))
      }
    }

    class VersionedIdDeserializer extends JsonDeserializer[VersionedId[ObjectId]] {
      def deserialize(jp: JsonParser, context: DeserializationContext): VersionedId[ObjectId] = {
        import models.versioning.VersionedIdImplicits.Binders._
stringToVersionedId(jp.getText).get
      }
    }

    object CorespringJson extends com.codahale.jerkson.Json {
      val module = new SimpleModule("CorespringJson", Version.unknownVersion())
      module.addSerializer(classOf[ObjectId], new ObjectIdSerializer)
      module.addSerializer(classOf[VersionedId[_]], new VersionedIdSerializer)
      module.addDeserializer(classOf[ObjectId], new ObjectIdDeserializer)
      module.addDeserializer(classOf[VersionedId[_]], new VersionedIdDeserializer)
      mapper.registerModule(module)
    }

  }

}