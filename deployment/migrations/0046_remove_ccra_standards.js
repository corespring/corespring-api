function isCCRA(dotNotation) {
  return dotNotation.indexOf("CCRA") == 0;
}

function up() {
  db.ccstandards.find().forEach(function(standard) {
    if (isCCRA(standard.dotNotation)) {
      db.ccstandards.remove(standard);
    }
  });
}

function down() {
  // no
}