package org.corespring.container.client.controllers.helpers

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.ExecutionContext
import play.api.libs.json.Json._

class XmlItemInspectorTest extends Specification with Mockito {

  trait scope extends Scope {

    val processor = {
      val m = mock[XhtmlProcessor]
      m.toWellFormedXhtml(any[String], any[String]).answers { (args, _) =>
        args.asInstanceOf[Array[Any]](0).asInstanceOf[String]
      }
      m
    }
    val check = new XmlItemInspector(processor, ExecutionContext.global)
  }

  "findComponentsNotInXhtml" should {

    trait findComponentsNotInXhtml extends scope {
      def comps = obj("1" -> obj("componentType" -> "comp"))
    }

    "find 1 component not in xhtml" in new findComponentsNotInXhtml {
      val result = check.findComponentsNotInXhtml("<div></div>", comps)
      result must equalTo(Seq("1" -> (comps \ "1"))).await
    }

    "find 1 components not in xhtml if the id is wrong" in new findComponentsNotInXhtml {
      val result = check.findComponentsNotInXhtml("<div><comp id='2'></comp></div>", comps)
      result must equalTo(Seq("1" -> (comps \ "1"))).await
    }

    "find 1 components not in xhtml if the component name is wrong" in new findComponentsNotInXhtml {
      val result = check.findComponentsNotInXhtml("<div><compp id='1'></compp></div>", comps)
      result must equalTo(Seq("1" -> (comps \ "1"))).await
    }

    "find 0 components not in xhtml" in new findComponentsNotInXhtml {
      val result = check.findComponentsNotInXhtml("<div><comp id='1'></comp></div>", comps)
      result must equalTo(Seq.empty).await
    }

    "find 0 components not in xhtml when component is declared as an attribute" in new findComponentsNotInXhtml {
      val result = check.findComponentsNotInXhtml("<div><div comp='' id='1'></div></div>", comps)
      result must equalTo(Seq.empty).await
    }

    "find 1 component not in xhtml when multiple components are defined" in new findComponentsNotInXhtml {
      override val comps = obj("1" -> obj("componentType" -> "comp"), "2" -> obj("componentType" -> "comp-two"))
      val result = check.findComponentsNotInXhtml("<div><div comp='' id='1'></div></div>", comps)
      result must equalTo(Seq("2" -> (comps \ "2"))).await
    }

  }

}
