// Run this with Node
// node add_grades_to_standards.js

var StandardsHelper = require('./standards_helper');

(function() {
  var fs = require('fs');

  var newStandards = [];
  fs.readFileSync('../../conf/seed-data/common/ccstandards.json').toString().split('\n').forEach(function(line) {
    if (line) {
      var standard = JSON.parse(line);
      if (StandardsHelper.getGrades(standard.dotNotation)) {
        standard.grades = StandardsHelper.getGrades(standard.dotNotation);
      }
      newStandards.push(JSON.stringify(standard));
    }
  });

  fs.writeFile('../../conf/seed-data/common/ccstandards.json', newStandards.join('\n'), function(err) {
    if (err) {
      console.log(err);
    } else {
      console.log("Added grades to standards.");
    }
  });

})();
