/* global KdsSetDefaultTitlesIfTitleEmpty */

var sut, item;

function createValidItem() {
  return {
    _id: {
      _id: "123"
    },
    taskInfo: {
      title: "title",
      description: "description",
      extended: {
        kds: {
          sourceId: "456",
          scoringType: "ABC"
        }
      }
    }
  };
}

function init() {
  sut = new KdsSetDefaultTitlesIfTitleEmpty();
  item = createValidItem();
}

function getUpdate(item){
  var update = sut.checkIfUpdateIsNeeded(item);
  //for testing we don't need the item
  delete update.item;
  return update;
}

describe("main class", function () {
  it("should be able to instantiate", function () {
    init();
    expect(sut).toBeDefined();
  });
});

describe("title is empty", function () {

  beforeEach(function () {
    init();
    delete item.taskInfo.title;
  });

  it("should set title to [sourceId] - [scoringType]", function () {
    var update = getUpdate(item);
    expect(update).toEqual({itemId: {_id: '123'}, type: "UPDATE", updates: {"taskInfo.title": '456 - ABC'}});
  });

});

describe("title is not empty", function () {
  beforeEach(init);

  it("should not change a normal title", function () {
    var update = getUpdate(item);
    expect(update).toEqual({itemId: {_id: '123'}, type: "NO CHANGE", updates:{}});
  });
});

describe("description is empty", function () {
  beforeEach(function () {
    init();
    delete item.taskInfo.description;
  });

  it("should set description to [sourceId] - [scoringType]", function () {
    var update = getUpdate(item);
    expect(update).toEqual({itemId: {_id: '123'}, type: "UPDATE", updates: {"taskInfo.description": '456 - ABC'}});
  });

  it("should use sourceId from title, if kds.sourceId is not set", function () {
    item.taskInfo.title = "789 - ABC Something else";
    delete item.taskInfo.extended.kds.sourceId;

    var update = getUpdate(item);

    expect(update).toEqual({
      itemId: {_id: '123'},
      type: "UPDATE",
      updates: {"taskInfo.description": '789 - ABC', "taskInfo.extended.kds.sourceId": "789"}
    });
  });
});

describe("description is not empty", function () {
  beforeEach(init);

  it("should not change the description", function () {
    var update = getUpdate(item);
    expect(update).toEqual({itemId: {_id: '123'}, type: "NO CHANGE", updates:{}});
  });
});

describe("sourceId is empty", function () {
  beforeEach(function () {
    init();
    delete item.taskInfo.extended.kds.sourceId;
  });

  it("should update sourceId from title", function () {
    item.taskInfo.title = "789";
    var update = getUpdate(item);
    expect(update).toEqual({
      itemId: {_id: '123'},
      type: "UPDATE",
      updates: {"taskInfo.extended.kds.sourceId": "789"}
    });
  });

  it("should not update sourceId, if title does not start with a sourceId", function () {
    item.taskInfo.title = "title";
    var update = getUpdate(item);
    expect(update).toEqual({itemId: {_id: '123'}, type: "NO CHANGE", updates:{}});
  });
});

