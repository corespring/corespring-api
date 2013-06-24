package common.models

import play.api.libs.json.{Reads, JsValue, Writes}
import play.api.libs.json.Json._
import org.codehaus.jackson.map.module.SimpleModule
import org.corespring.platform.data.mongo.models.VersionedId


package object json {

  /**
   * Convenience Reads/Writes that use jerkson instead of having to manually serialize the data.
   */
  object jerkson {

    trait JerksonWrites[A] extends Writes[A] {
      def writes(c: A) = {
        play.api.libs.json.Json.parse(CorespringJson.generate(c))
      }
    }

    trait JerksonReads[A] extends Reads[A] {

      //TODO: Any way we can avoid having to do this?
      def manifest : Manifest[A]

      def reads(js: JsValue ): A = {
        CorespringJson.parse[A](stringify(js))(manifest)
      }
    }

    import org.codehaus.jackson.map.annotate.JsonCachable
    import org.bson.types.ObjectId
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
    class VersionedIdSerializer extends JsonSerializer[VersionedId[ObjectId]] {
      def serialize(id:VersionedId[ObjectId], json : JsonGenerator, provider : SerializerProvider) {
        import models.versioning.VersionedIdImplicits.Binders._
        json.writeString( versionedIdToString(id.asInstanceOf[VersionedId[ObjectId]]))
      }
    }

    class VersionedIdDeserializer extends JsonDeserializer[VersionedId[ObjectId]] {
      def deserialize(jp : JsonParser, context : DeserializationContext) : VersionedId[ObjectId] = {
        import models.versioning.VersionedIdImplicits.Binders._
        stringToVersionedId(jp.getText).get
      }
    }

    object CorespringJson extends com.codahale.jerkson.Json {
      val module = new SimpleModule("CorespringJson", Version.unknownVersion())
      module.addSerializer( classOf[ObjectId], new ObjectIdSerializer)
      module.addSerializer( classOf[VersionedId[ObjectId]], new VersionedIdSerializer)
      module.addDeserializer(classOf[ObjectId], new ObjectIdDeserializer)
      module.addDeserializer(classOf[VersionedId[ObjectId]], new VersionedIdDeserializer)
      mapper.registerModule(module)
    }

  }

}