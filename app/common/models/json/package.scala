package common.models

import play.api.libs.json.{Reads, JsValue, Writes}
import play.api.libs.json.Json._
import org.codehaus.jackson.map.module.SimpleModule
import play.Logger


package object json {

  /**
   * Convenience Reads/Writes that use jerkson instead of having to manually serialize the data.
   */
  object jerkson {

    trait JerksonWrites[A] extends Writes[A] {
      def writes(c: A) = {
        Logger.debug("JerksonWrites: " + c)
        play.api.libs.json.Json.parse(CorespringJson.generate(c))
      }
    }

    trait JerksonReads[A] extends Reads[A] {

      def manifest : Manifest[A]

      def reads(js: JsValue ): A = {
        Logger.debug("JerksonReads: " + js)
        Logger.debug("A: " )
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

    object CorespringJson extends com.codahale.jerkson.Json {
      val module = new SimpleModule("CorespringJson", Version.unknownVersion())
      module.addSerializer(classOf[ObjectId], new ObjectIdSerializer)
      module.addDeserializer(classOf[ObjectId], new ObjectIdDeserializer)
      mapper.registerModule(module)
    }

  }

}