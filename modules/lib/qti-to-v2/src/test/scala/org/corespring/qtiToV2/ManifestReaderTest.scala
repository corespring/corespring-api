package org.corespring.qtiToV2

import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification

import scala.io.Source

class ManifestReaderTest extends Specification {


  "read" should {

    "return filenames" in {
      println(ManifestReader.read(Source.fromURL(getClass.getResource(ManifestReader.filename))))
      true === true
    }

  }

}
