package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification

class TeacherInstructionsTransformerTest extends Specification {

  "TeacherInstructionsTransformer" should {

    val teacherInstructions = "Don't let the kids run wild."

    def qti(teacherInstructions: String) =
      <assessmentItem>
        <itemBody>
          <partBlock label="teacherInstructions">{teacherInstructions}</partBlock>
        </itemBody>
      </assessmentItem>

    val result = TeacherInstructionsTransformer.interactionJs(qti(teacherInstructions))

    "contain teacher instructions" in {
      (result.values.headOption.getOrElse(throw new Exception("Result was empty")) \ "value").as[String] must be equalTo(teacherInstructions)
    }

  }

}
