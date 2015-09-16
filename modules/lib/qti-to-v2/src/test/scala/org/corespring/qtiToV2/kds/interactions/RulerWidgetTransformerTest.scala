package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification

import scala.xml.Node

class RulerWidgetTransformerTest extends Specification {

  import RulerWidgetTransformer._

  def manifestFor(node: Option[Node]) =
    <resource>
      <metadata>
        <lom>
          <general>
            <mathTools>
              { node.getOrElse("") }
            </mathTools>
          </general>
        </lom>
      </metadata>
    </resource>

  def ruler(units: String = Defaults.Units, label: String = Defaults.Label, length: Int = Defaults.Length,
    pixelsPerUnit: Int = Defaults.PixelsPerUnit, ticks: Int = Defaults.Ticks) =
    <ruler units={ units } label={ label } length={ length.toString } pixelsPerUnit={ pixelsPerUnit.toString } ticks={ ticks.toString }></ruler>

  "interactionJs" should {

    "return nothing when there is no ruler" in {
      interactionJs(<qti></qti>, manifestFor(None)) must be equalTo (Map.empty)
    }

    "return JSON with specified values when there is a ruler defined" in {
      val units = "metric"
      val label = "cm"
      val length = 10
      val pixelsPerUnit = 40
      val ticks = 100

      val config = interactionJs(<qti></qti>, manifestFor(Some(ruler(units, label, length, pixelsPerUnit, ticks))))
        .get(id).getOrElse(throw new RuntimeException(s"There was no component for $id")) \ "model" \ "config"

      (config \ "units").as[String] must be equalTo (units)
      (config \ "label").as[String] must be equalTo (label)
      (config \ "length").as[Int] must be equalTo (length)
      (config \ "pixelsPerUnit").as[Int] must be equalTo (pixelsPerUnit)
      (config \ "ticks").as[Int] must be equalTo (ticks)
    }

    "return JSON with default values when there is a ruler without values defined" in {

      val config = interactionJs(<qti></qti>, manifestFor(Some(<ruler></ruler>))).get(id)
        .getOrElse(throw new RuntimeException(s"There was no component for $id")) \ "model" \ "config"

      (config \ "units").as[String] must be equalTo (Defaults.Units)
      (config \ "label").as[String] must be equalTo (Defaults.Label)
      (config \ "length").as[Int] must be equalTo (Defaults.Length)
      (config \ "pixelsPerUnit").as[Int] must be equalTo (Defaults.PixelsPerUnit)
      (config \ "ticks").as[Int] must be equalTo (Defaults.Ticks)
    }

  }

  "transform" should {

    val qti = <itemBody></itemBody>

    "add no child to <itemBody/> when no ruler is defined" in {
      transform(qti, manifestFor(None)) must be equalTo (qti)
    }

    "add <corespring-ruler/> child to <itemBody/> when ruler is defined" in {
      transform(qti, manifestFor(Some(<ruler></ruler>))) must be equalTo (<itemBody>{ rulerNode }</itemBody>)
    }

  }

}
