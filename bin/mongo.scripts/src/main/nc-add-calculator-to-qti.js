//a script that adds the calculator to the top of certain nc items
//see comments at the bottom when you want to run it

/* global db */
function NcCalculatorAdder(doUpdateData, ignoredCollections) {

  doUpdateData = doUpdateData === true;
  ignoredCollections = ignoredCollections || [];

  var self = this;
  self.run = run;
  self.convertQti = convertQti; //make public for easier testing

  //------------------------------------

  function run() {
    processItems("basic", findBasicCalculatorItems());
    processItems("scientific", findScientificCalculatorItems());
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

  function processItems(type, items) {
    log("processItems: " + type + " -- does update data:" + doUpdateData);

    var updates = 0;
    var wasUpdatedAlready = 0;
    var count = items.count();
    log("processItems: #items to process:" + count);
    items.forEach(updateItem);
    log("processItems: #items processed:" + updates);

    function updateItem(item) {
      var qti = getQti(item);
      if (!qti) {
        return;
      }
      var updatedQti = convertQti(qti, item, type);
      if (updatedQti) {
        if (doUpdateData) {
          var query = {
            "_id": item._id,
            "data.files.name": "qti.xml" //needed for the positional operator in the update
          };
          var update = {
            "$set": {
              "data.files.$.content": updatedQti
            },
            "$unset": {
              "playerDefinition": ""
            }
          };
          db.content.update(query, update);
        }
        updates++;
      }
    }
  }

  function convertQti(qti, item, type) {
    var itemType1 = /<itemBody>/gi;
    var itemType2 = /<div class="item-body">/gi;
    var calculatorId = "automatically-inserted-calculator";
    var calculatorXhtml = "<csCalculator responseIdentifier=\"" + calculatorId + "\" type=\"" + type + "\"></csCalculator>";

    qti = qti.replace(/<i>\s*You may use a calculator to answer this question\.\s*<\/i>/gi, "");
    qti = qti.replace(/You may use a calculator to answer this question\./gi, "");
    qti = qti.replace(/<csCalculator.+\/csCalculator>/gi, "");

    if (qti.search(itemType1) >= 0) return qti.replace(itemType1, "<itemBody>" + calculatorXhtml);
    if (qti.search(itemType2) >= 0) return qti.replace(itemType2, "<div class=\"item-body\">" + calculatorXhtml);

    log("WARNING: Unexpected format of qti for item " + itemIdToString(item) + " qti:" + qti);
    return null;
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
    return db.content.find(query, {
      "data.files": 1
    })
  }

  function getQti(item) {
    if (!item.data) {
      logCouldNotFind("data", item);
      return "";
    }
    if (!item.data.files) {
      logCouldNotFind("files", item);
      return "";
    }
    var files = item.data.files;
    for (var i = 0; i < files.length; i++) {
      var file = files[i];
      if (file && file.name === "qti.xml") {
        var content = file.content || "";
        if (!content) {
          logCouldNotFind("qti content", item);
          return "";
        } else {
          return content;
        }
      }
    }
    logCouldNotFind("qti file", item);
    return ""
  }


  function itemIdToString(item) {
    return item._id._id + ":" + item._id.version;
  }

  function logCouldNotFind(property, item) {
    log("WARNING: could not find " + property + " for " + itemIdToString(item));
  }

  function log(message, json) {
    print(message);
    if (json) {
      printjson(json);
    }

  }
}

//by default updates are disabled for safety reasons
//pass in "true" to make it update the data
//pass in the ids of the collections that should be excluded, eg. the archive collection
var archiveColl = "500ecfc1036471f538f24bdc"; //same fore staging and prod
var publicSiteSamplesColl = "541b00bd5966943aed30daf9"; //same fore staging and prod
var processor = new NcCalculatorAdder(false, [archiveColl, publicSiteSamplesColl]);
processor.run();