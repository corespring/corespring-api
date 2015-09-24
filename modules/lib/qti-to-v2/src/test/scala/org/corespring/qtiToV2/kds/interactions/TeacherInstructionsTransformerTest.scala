package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.transformers.ItemTransformer
import org.specs2.mutable.Specification

class TeacherInstructionsTransformerTest extends Specification {

  "TeacherInstructionsTransformer" should {

    "version 1" should {
      val teacherInstructions = "Don't let the kids run wild."
      def qti(teacherInstructions: String) =
        <assessmentItem>
          <itemBody>
            <partBlock label="teacherInstructions">{teacherInstructions}</partBlock>
          </itemBody>
        </assessmentItem>



      val xml = qti(teacherInstructions)
      val partBlock = (xml \\ "partBlock").headOption.getOrElse(throw new Exception("There were no instructions"))

      val result = TeacherInstructionsTransformer.interactionJs(xml, ItemTransformer.EmptyManifest)
      val xmlResult = TeacherInstructionsTransformer.transform(partBlock, ItemTransformer.EmptyManifest)
        .headOption.getOrElse(throw new Exception("Result was empty"))
      val json = result.values.headOption.getOrElse(throw new Exception("Result was empty"))
      val id = result.keys.headOption.getOrElse(throw new Exception("Result was empty"))
      val resultId = TeacherInstructionsTransformer.teacherInstructionsId(partBlock)

      "contain teacher instructions" in {
        (json \ "teacherInstructions").as[String] must be equalTo (teacherInstructions)
        id must be equalTo (resultId)
      }

      "transform <partBlock label='teacherInstructions'/> to <corespring-teacher-instructions/>" in {
        xmlResult.label must be equalTo "corespring-teacher-instructions"
        (xmlResult \ "@id").text.toString must be equalTo (resultId)
      }
    }

    "version 2" should {

      val teacherInstructions = "<![CDATA[<strong>TEACHER READS:</strong><br /><br />Read and complete the task that follows.]]>"
      val xml = <assessmentItem>
        <itemBody>
          <teacherInstructions>{teacherInstructions}</teacherInstructions>
        </itemBody>
      </assessmentItem>

      val instructions = (xml \\ "teacherInstructions").headOption.getOrElse(throw new Exception("There were no instructions"))
      val result = TeacherInstructionsTransformer.interactionJs(xml, ItemTransformer.EmptyManifest)
      val xmlResult = TeacherInstructionsTransformer.transform(instructions, ItemTransformer.EmptyManifest)
        .headOption.getOrElse(throw new Exception("Result was empty"))
      val json = result.values.headOption.getOrElse(throw new Exception("Result was empty"))
      val id = result.keys.headOption.getOrElse(throw new Exception("Result was empty"))
      val resultId = TeacherInstructionsTransformer.teacherInstructionsId(instructions)

      "contain teacher instructions" in {
        (json \ "teacherInstructions").as[String] must be equalTo (teacherInstructions)
        id must be equalTo (resultId)
      }

      "transform <partBlock label='teacherInstructions'/> to <corespring-teacher-instructions/>" in {
        xmlResult.label must be equalTo "corespring-teacher-instructions"
        (xmlResult \ "@id").text.toString must be equalTo (resultId)
      }
    }

  }

}
