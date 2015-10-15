/* global NcCalculatorAdder */

var sut, item;

function createValidItem() {
  return {
    _id: {
      _id: "123"
    },
    data: {
      files: [
        {
          name: "qti.xml",
          content: ""
        }
      ]
    },
    taskInfo: {
      title: "title",
      description: "description",
      extended: {
        new_classrooms: {
          skillNumber: "222"
        }
      }
    }
  };
}

function init() {
  sut = new NcCalculatorAdder();
  item = createValidItem();
}

describe("main class", function () {
  it("should be able to instantiate", function () {
    init();
    expect(sut).toBeDefined();
  });
});

describe("qti conversion", function () {
  var convertQtiFn;

  beforeEach(function () {
    init();
  });

  it("should add basic calculator after <itemBody>", function () {
    var qti = "<itemBody>";
    var update = sut.convertQti(qti, item, "basic");
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="basic"></csCalculator>');
  });

  it("should add scientific calculator after <itemBody>", function () {
    var qti = "<itemBody>";
    var update = sut.convertQti(qti, item, "scientific");
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="scientific"></csCalculator>');
  });

  it("should remove 'You may use calculator'", function () {
    var qti = "<itemBody>You may use a calculator to answer this question.";
    var update = sut.convertQti(qti, item, "scientific");
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="scientific"></csCalculator>');
  });

  it("should remove '<i>You may use calculator</i>'", function () {
    var qti = "<itemBody><i>You may use a calculator to answer this question.</i>";
    var update = sut.convertQti(qti, item, "scientific");
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="scientific"></csCalculator>');
  });

  it("should remove existing calculators", function () {
    var qti = '<itemBody><csCalculator some="thing"></csCalculator>';
    var update = sut.convertQti(qti, item, "scientific");
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="scientific"></csCalculator>');
  });

  it("should add basic calculator after <div class=\"item-body\">", function () {
    var qti = "<div class=\"item-body\">";
    var update = sut.convertQti(qti, item, "basic");
    expect(update).toEqual('<div class=\"item-body\"><csCalculator responseIdentifier="automatically-inserted-calculator" type="basic"></csCalculator>');
  });

});

