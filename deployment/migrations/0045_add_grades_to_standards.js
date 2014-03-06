load("bin/javascript.scripts/standards_helper.js");

function up() {
  var grades = module.exports;
  db.ccstandards.find().forEach(function(standard) {
    if (standard.dotNotation && grades.getGrades(standard.dotNotation)) {
      standard.grades = grades.getGrades(standard.dotNotation);
      db.ccstandards.save(standard);
    }
  });
}

function down() {
  db.ccstandards.find().forEach(function(standard) {
    delete standard.grades;
    db.ccstandards.save(standard);
  });
}