//a script that adds the calculator to the top of certain nc items

/* global db */

function NcCalculatorAdder() {

  var self = this;
  self.run = run;
  self.convertQti = convertQti; //for testing

  //------------------------------------

  function run(){
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
    log("processItems: " + type);

    var updates = 0;
    var wasUpdatedAlready = 0;
    var count = items.count();

    items.forEach(updateItem);
    log("processItems: items processed total:" + count + " updated first time: " + updates + " updated again: " + wasUpdatedAlready);

    function updateItem(item) {
      var qti = getQti(item);
      if (!qti) {
        return;
      }
      if(qti.search(calculatorRegExp) >= 0){
        wasUpdatedAlready++;
      }
      var updatedQti = convertQti(qti, item, type);
      if (updatedQti) {
        db.content.update({
          "_id": item._id,
          "data.files.name": "qti.xml",
        }, {
          "$set": {
            "data.files.$.content": updatedQti
          }
        });
        updates++;
      }
    }
  }

  function convertQti(qti, item, type) {
    var itemType1 = /<itemBody>/gi;
    var itemType2 = /<div class="item-body">/gi;
    var calculatorId = "automatically-inserted-calculator";
    var calculatorXhtml = "<csCalculator responseIdentifier=\"" + calculatorId + "\" type=\"" + type + "\"></csCalculator>";

    qti = qti.replace(/You may use a calculator to answer this question\./, "");
    qti = qti.replace(/<i><\/i>/, "");
    qti = qti.replace(/<csCalculator.+\/csCalculator>/gi, "");
    if (qti.search(itemType1) >= 0) return qti.replace(itemType1, "<itemBody>" + calculatorXhtml);
    if (qti.search(itemType2) >= 0) return qti.replace(itemType2, "<div class=\"item-body\">" + calculatorXhtml);
    log("WARNING: Unexpected format of qti for item " + itemIdToString(item) + " qti:" + qti);
    return null;
  }

  function findCalculatorItemsBySkillNumber(skillNumbers) {
    return db.content.find({
      "taskInfo.extended.new_classrooms.skillNumber": {
        "$in": skillNumbers
      }
    }, {"data.files":1})
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

  function log(message) {
    print(message);
  }

}

//var processor = new NcCalculatorAdder();
//processor.run();

