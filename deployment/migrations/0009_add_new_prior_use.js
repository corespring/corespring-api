var newElement = {
  "key": "International Benchmark",
  "value": "International Benchmark"
};

function up() {
  db.fieldValues.find().forEach(function (it) {
    it.priorUses.push(newElement);
    db.fieldValues.save(it);
  });
}

function down() {
  db.fieldValues.find().forEach(function (it) {
    var priorUses = [];
    it.priorUses.forEach(function(e) {
      if (e.key != "International Benchmark") {
        priorUses.push({key: e.key, value: e.value});
      }
    });
    it.priorUses = priorUses;
    db.fieldValues.save(it);
  });
}
