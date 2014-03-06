load("bin/javascript.scripts/standards_helper.js");

function up() {
  var StandardsHelper = module.exports;
  db.ccstandards.find().forEach(function(standard) {
    if (StandardsHelper.isCCRA(standard.dotNotation)) {
      db.ccstandards.remove(standard);
    }
  });
}

function down() {
  // no
}