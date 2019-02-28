package org.corespring.container.client.controllers

import org.corespring.container.client.hooks.{ AssetHooks, UploadResult }
import org.corespring.test.TestContext
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.mvc._
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.Future

class AssetsControllerTest extends PlaySpecification with Mockito {

  trait scope extends Scope with TestContext {

    val h: AssetHooks = {
      val m = mock[AssetHooks]
      m
    }

    val controller = new AssetsController[AssetHooks] with TestContext {
      override def hooks: AssetHooks = h

      override def getTimestamp = "stamp"

      override def acceptableSuffixes: Seq[String] = Seq("png")
    }

    def rawRequest(path: String): Request[AnyContentAsRaw] = {
      FakeRequest("POST", path).withRawBody(Array.empty[Byte])
    }
  }

  "uploadFile" should {

    trait uploadFile extends scope {
      val action = controller.uploadFile("id", "path.png")
      val r: Request[Future[UploadResult]] = FakeRequest().withBody(Future.successful(UploadResult("upload-result-stamp-path.png")))
      val result: Future[SimpleResult] = action.apply(r)
    }

    "call hooks.upload with timestamped path" in new uploadFile {
      import org.mockito.Matchers.{ eq => m_eq }
      there was one(h).upload(m_eq("id"), m_eq("stamp-path.png"))(any[RequestHeader => Option[SimpleResult]])
    }

    "returns the upload result path" in new uploadFile {
      contentAsString(result) must_== "upload-result-stamp-path.png"
    }
  }

  "acceptableType" should {

    s"return an $BAD_REQUEST if there is no suffix" in new scope {
      val result = controller.acceptableType(FakeRequest("", "path"))
      contentAsString(Future(result.get)) must_== "Unknown file suffix for path: path"
    }

    s"return an $BAD_REQUEST if the suffix is not acceptable" in new scope {
      val result = controller.acceptableType(FakeRequest("", "path.sfx"))
      contentAsString(Future(result.get)) must_== "Unsupported suffix: sfx"
    }

    s"return an None if the suffix is acceptable" in new scope {
      controller.acceptableType(FakeRequest("", "path.png")) must_== None
    }

    s"return an None if the suffix is upppercase and acceptable" in new scope {
      controller.acceptableType(FakeRequest("", "path.PNG")) must_== None
    }
  }
}
