package org.corespring.api.v1

import play.api.mvc.Controller

class FieldValuesApi(v2: org.corespring.v2.api.FieldValuesApi) extends Controller {

  def domain = v2.domain

  def subject = v2.subject

  def standard = v2.standard
}
