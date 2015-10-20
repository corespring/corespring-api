package org.corespring.common.util.xml

/**
 * Cleans up XML strings by running them through a collection of "cleaner" transformations
 */
object XMLCleaner {

  /**
   * Seq of String => String functions that clean XML
   *
   * NOTE: If you decide to add anything that substantially changes XML to this, please note that you will also have to
   * update the data on the client.
   */
  private val cleaners: Seq[String => String] = Seq(
    (xml: String) => xml.trim)

  /**
   * Runs throw all the cleaner functions and returns the result XML
   */
  def clean(xml: String): String = cleaners.foldLeft(xml)((xml, cleaner) => cleaner(xml))

}
