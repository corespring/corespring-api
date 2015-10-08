package org.corespring.qtiToV2.kds

import org.corespring.qtiToV2.kds.responseProcessing.FieldValueProcessingTransformer
import org.specs2.mutable.Specification

class FieldValueProcessingTransformerTest extends Specification {
  import scala.xml.Utility.trim

  val identifier = "RESPONSE1"

  "transform" should {

    "replace <equals><fieldValue/></equals> with <equals><variable/><correct/></equals>" in {
      val expression =
        <equal>
          <fieldValue fieldIdentifier="startPointXCoordinate">
            <variable identifier={identifier}/>
          </fieldValue>
          <baseValue baseType="float">0</baseValue>
        </equal>

      trim(FieldValueProcessingTransformer.transform(expression)) must be equalTo (
        trim(<equal>
          <variable identifier={identifier}/>
          <correct identifier={identifier}/>
        </equal>))
    }

    "simplify duplicates in <and/>" in {
      val expression =
        <and>
          <equal>
            <variable identifier={identifier}/>
            <correct identifier={identifier}/>
          </equal>
          <equal>
            <variable identifier={identifier}/>
            <correct identifier={identifier}/>
          </equal>
        </and>

      trim(FieldValueProcessingTransformer.transform(trim(expression))) must be equalTo(trim(
        <equal>
          <variable identifier={identifier}/>
          <correct identifier={identifier}/>
        </equal>
      ))
    }

    "simplify duplicates in <or/>" in {
      val expression =
        <or>
          <equal>
            <variable identifier={identifier}/>
            <correct identifier={identifier}/>
          </equal>
          <equal>
            <variable identifier={identifier}/>
            <correct identifier={identifier}/>
          </equal>
        </or>

      trim(FieldValueProcessingTransformer.transform(trim(expression))) must be equalTo(trim(
        <equal>
          <variable identifier={identifier}/>
          <correct identifier={identifier}/>
        </equal>
      ))
    }

  }

}
