package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification

class XMLNamespaceClearerTest extends Specification with XMLNamespaceClearer {

  "clearNamespace" should {

    val xml =
      <parent class="test" xmlns="this is my namespace!" xmlns:xsi="this is my namespace!">
        <child xmlns="I've got one too!" xmlns:xsi="I've got one too!"/>
      </parent>

    val result = clearNamespace(xml)

    "remove namespace from all nodes" in {
      result.scope.toString must beEmpty
      result.child.map(_.scope.toString).flatten must beEmpty
    }

  }

}
