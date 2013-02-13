package tests.models

import tests.BaseTest


class VersioningTest extends BaseTest{

  "saving an item resource with no sessions returns successfully" in {
    //use /api/v1/items/:itemId/data/qti.xml
    pending
  }
  "saving an item resource with no changes returns successfully" in {
    //use /api/v1/items/:itemId/data/qti.xml
    pending
  }
  "saving an item resource with no sessions returns successfully" in {
    //use /api/v1/items/:itemId/data/qti.xml
    pending
  }
  "saving an item resource with changes and sessions but with a force query field returns successfully" in {
    //use /api/v1/items/:itemId/data/qti.xml
    pending
  }
  "saving an item resource with changes and sessions and with no force query field returns a forbidden message" in {
    pending
  }
  "incrementing an item and then querying for a list of items returns the new revision of that item and does not include the old" in {
    pending
  }
}
