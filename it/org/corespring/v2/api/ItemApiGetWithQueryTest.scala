package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification

/**
 * Note: Below are assertions take from the old v1 <test.models.ItemQueryTest>.
 * It has been removed as it was dependent on seeding the db for an entire test run (something we don't do anymore).
 * I'm not sure if they are still valid assertions with the v2 api search.
 * Will have to go through them and remove those that aren't, add any new assertions
 * then implement the assertion body.
 */
class ItemApiGetWithQueryTest extends IntegrationSpecification {

  "item api GET with query" should {
    "filter search for author in contributorDetails" in pending
    "filter search by gradeLevel matching multiple grades" in pending
    "filter search by subCategory in standards" in pending
    "filter search by subjects" in pending
    "use regex to filter search by title" in pending
    "filter search by itemType AND title" in pending
    "filter search by title OR primarySubject OR gradeLevel OR itemType OR standards.dotNotation OR contributorDetails.author" in pending
    """search by title OR
                 primarySubject OR
                 gradeLevel OR
                 itemType OR
                 standards.dotNotation
                 OR contributorDetails.author
                 returns results even with a value for standards.dotNotation that does not contain any results""" in pending
      "filtering by supportingMaterials results in error" in pending
      "filtering by data results in error" in pending
      "filter items based on a set of subjects using $in" in pending
      "filter items based on a set of collections using $in" in pending
      "all queryable item params are checked within it's search method" in pending
      "all search field item params are checked within it's search method" in pending
      "search for items that do not have an itemType equal to Multiple Choice" in pending
      "search for items that do not contain a gradeLevel of 02 or 04" in pending
      "no results are returned when searching for itemType that has both values of Multiple Choice and Project" in pending
      "match all items that have a gradeLevel which contains the values 02 and 04 in its array" in pending
      "search for items by metadata" in pending
    }
  }
