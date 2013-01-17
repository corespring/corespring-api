package tests.models

import tests.BaseTest


class ItemQueryTest extends BaseTest{

  "filter search for contributorDetails" in {
    pending
  }
  "filter search by gradeLevel" in {
    pending
  }
  "filter search by standards" in {
    pending
  }
  "filter search by subjects" in {
    pending
  }
  "use regex to filter search by title" in {
    pending
  }
  "filter search by lexile AND originId" in {
    pending
  }
  "filter search by workflow OR bloomsTaxonomy" in  {
    pending
  }
  "filtering by supportingMaterials or data results in error" in {
    pending
  }
  "filter items based on a set of subjects using $in" in {
    pending
  }
  "filter items that are not included in a certain collection using $ne" in {
    pending
  }
  "all queryable item params are checked within it's search method" in {
    pending
  }
  "all search field item params are checked within it's search method" in {
    pending
  }
  "search items, excluding 'standards.subject' includes everything except the subject portion of the standard" in {
    pending
  }
  "search items, excluding 'primarySubject.category' includes everything except the category portion of the primary subject" in {
    pending
  }
}
