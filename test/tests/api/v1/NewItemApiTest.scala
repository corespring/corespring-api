package tests.api.v1

import tests.BaseTest
import models.item.Item


class NewItemApiTest extends BaseTest{
  step {
    itemService.insert(Item(

    ))
  }

  "cloning an item" should {
    "contain the same data as the cloned item (excluding the id)" in {
      pending
    }
    "contain a different id from the cloned item" in {
      pending
    }
    "contain stored files with different id's in the storage key's" in {
      pending
    }
  }

  step {

  }

  "updating an item that is published and contains responses" should {
    "be identifiable as a versioned item" in {
      pending
    }
    "be able to retrieve version history" in {
      pending
    }
  }
}
