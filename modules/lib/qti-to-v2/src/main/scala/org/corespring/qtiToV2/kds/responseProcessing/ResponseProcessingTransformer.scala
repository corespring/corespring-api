package org.corespring.qtiToV2.kds.responseProcessing

import scala.xml.Node

abstract class ResponseProcessingTransformer {

  def transform(node: Node): Node

}

object ResponseProcessingTransformer {

  val all = Seq(
    FieldValueProcessingTransformer
  )

  def transformAll(node: Node): Node = all.foldLeft(node){ case (acc, transformer) => transformer.transform(acc) }

}