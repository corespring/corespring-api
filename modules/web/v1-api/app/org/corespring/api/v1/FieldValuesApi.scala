package org.corespring.api.v1

import org.corespring.platform.core.controllers.auth.BaseApi

class FieldValuesApi(v2: org.corespring.v2.api.FieldValuesApi) extends BaseApi {

  def domain = v2.domain
}
