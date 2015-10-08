package org.corespring.qtiToV2.kds

/**
 * It's a nightmare to get corespring-batch-importer to understand nested folders for resources. I gave up. It's simpler
 * just to have the import process flatten all the resources.
 */
object PathFlattener {

  implicit class Flattener(path: String) {

    def flattenPath = path.split("/").last

  }

}
