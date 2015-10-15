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
    convertQtiFn = sut.makeQtiConvertFn("basic");
    var update = convertQtiFn(qti, item);
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="basic"></csCalculator>');
  });

  it("should add scientific calculator after <itemBody>", function () {
    var qti = "<itemBody>";
    convertQtiFn = sut.makeQtiConvertFn("scientific");
    var update = convertQtiFn(qti, item);
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="scientific"></csCalculator>');
  });

  it("should remove 'You may use calculator'", function () {
    var qti = "<itemBody>You may use a calculator to answer this question.";
    convertQtiFn = sut.makeQtiConvertFn("scientific");
    var update = convertQtiFn(qti, item);
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="scientific"></csCalculator>');
  });

  it("should remove '<i>You may use calculator</i>'", function () {
    var qti = "<itemBody><i>You may use a calculator to answer this question.</i>";
    convertQtiFn = sut.makeQtiConvertFn("scientific");
    var update = convertQtiFn(qti, item);
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="scientific"></csCalculator>');
  });

  it("should remove existing calculators", function () {
    var qti = '<itemBody><csCalculator some="thing"></csCalculator>';
    convertQtiFn = sut.makeQtiConvertFn("scientific");
    var update = convertQtiFn(qti, item);
    expect(update).toEqual('<itemBody><csCalculator responseIdentifier="automatically-inserted-calculator" type="scientific"></csCalculator>');
  });

  it("should add basic calculator after <div class=\"item-body\">", function () {
    var qti = "<div class=\"item-body\">";
    convertQtiFn = sut.makeQtiConvertFn("basic");
    var update = convertQtiFn(qti, item);
    expect(update).toEqual('<div class=\"item-body\"><csCalculator responseIdentifier="automatically-inserted-calculator" type="basic"></csCalculator>');
  });

});

