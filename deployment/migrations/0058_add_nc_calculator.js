var ignoredCollections = [];

function hasCalculator(item) {
  var component;
  if (item.playerDefinition && item.playerDefinition.components) {
    for (var i in item.playerDefinition.components) {
      component = item.playerDefinition.components[i];
      if (component.componentType === 'corespring-calculator') {
        return true;
      }
    }
  }
  return false;
}

function findCalculatorItemsBySkillNumber(skillNumbers) {
  var query = {
    "taskInfo.extended.new_classrooms.skillNumber": {
      "$in": skillNumbers
    }
  };
  if (ignoredCollections.length) {
    query.collectionId = {
      "$nin": ignoredCollections
    };
  }
  return db.content.find(query);
}

function findBasicCalculatorItems() {
  return findCalculatorItemsBySkillNumber(["222", "228", "302", "317", "355"]);
}

function findScientificCalculatorItems() {
  return findCalculatorItemsBySkillNumber(["127", "150", "173", "181", "191", "194", "200", "204", "205",
    "206", "207", "211", "219", "221", "224", "232", "236", "237", "240", "241", "242", "243",
    "244", "249", "251", "255", "259", "260", "261", "262", "266", "267", "273", "275", "280",
    "284", "285", "291", "299", "300", "308", "309", "319", "320", "322", "323", "324", "325",
    "326", "329", "331", "332", "333", "334", "335", "336", "337", "339", "340", "341", "342",
    "343", "345", "346", "347", "348", "349", "352", "354", "356", "358", "360", "363", "364",
    "365", "366", "371", "372", "374", "380", "381", "384", "385", "444", "503", "509", "511",
    "547", "548", "549", "550", "551", "553", "554", "555", "556", "557", "558", "559", "560",
    "561", "564", "565"]);
}

function addCalculator(item, calculatorType) {
  if (item.playerDefinition && item.playerDefinition.components) {
    item.playerDefinition.components['automatically-inserted-calculator'] = {
      "weight" : 0.0000000000000000,
      "title" : "Calculator",
      "isTool" : true,
      "componentType" : "corespring-calculator",
      "model" : {
        "config" : {
          "type" : calculatorType
        }
      }
    };
    item.playerDefinition.xhtml = item.playerDefinition.xhtml.replace("<itemBody>", "<itemBody>​<div id=\"automatically-inserted-calculator\" corespring-calculator=\"\"></div>​");
    return item;
  } else {
    return undefined;
  }
}

function run() {
  var itemWithCalculator;
  findBasicCalculatorItems().toArray().forEach(function(item) {
    if (!(hasCalculator(item))) {
      itemWithCalculator = addCalculator(item, "basic");
      if (itemWithCalculator) {
        db.content.save(itemWithCalculator);
      }
    }
  });

  findScientificCalculatorItems().toArray().forEach(function(item) {
    if (!(hasCalculator(item))) {
      itemWithCalculator = addCalculator(item, "scientific");
      if (itemWithCalculator) {
        db.content.save(itemWithCalculator);
      }
    }
  });
}

run();