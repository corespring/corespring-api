package tests.common.utils.string

import org.specs2.mutable.Specification
import common.utils.string

class StringPackageTest extends Specification {

  "string" should {

    "lower case first char" in {

      string.lowercaseFirstChar("Hello") === "hello"
      string.lowercaseFirstChar("") === ""
      string.lowercaseFirstChar(null) === null

    }

    "interpolate" in {
      string.interpolate("hello ${token}", string.replaceKey(Map("token" -> "world")), string.DollarRegex) === "hello world"


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

      val text = string.interpolate(policyTemplate, string.replaceKey(Map("bucket" -> "my-bucket")), string.DollarRegex)


      text.contains("my-bucket") === true

    }
  }

}
