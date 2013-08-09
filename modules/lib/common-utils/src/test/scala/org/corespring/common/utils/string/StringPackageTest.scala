package org.corespring.common.utils.string

import org.specs2.mutable.Specification

class StringPackageTest extends Specification {

  "string" should {

    "lower case first char" in {

      lowercaseFirstChar("Hello") === "hello"
      lowercaseFirstChar("") === ""
      lowercaseFirstChar(null) === null

    }

    "interpolate" in {
      interpolate("hello ${token}", replaceKey(Map("token" -> "world")), DollarRegex) === "hello world"


      val policyTemplate =
        """
          |{
          |  "Version":"2008-10-17",
          |  "Statement":[{
          |	"Sid":"AllowPublicRead",
          |		"Effect":"Allow",
          |	  "Principal": {
          |			"AWS": "*"
          |		 },
          |	  "Action":["s3:GetObject"],
          |	  "Resource":["arn:aws:s3:::${bucket}/*"
          |	  ]
          |	}
          |  ]
          |}
        """.stripMargin

      val text = interpolate(policyTemplate, replaceKey(Map("bucket" -> "my-bucket")), DollarRegex)


      text.contains("my-bucket") === true

    }
  }

}
