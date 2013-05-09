var newElement = {
  "key": "International Benchmark",
  "value": "International Benchmark"
};

function up() {
  var count = 0;
  db.content.find({"contributorDetails.contributor": "TIMSS"}).forEach(function (it) {
    count++;
    it.priorUse = "International Benchmark";
    db.content.save(it);
  });
  print("Updated " + count + " records");
}

function down() {
  var count = 0;
  db.content.find({"contributorDetails.contributor": "TIMSS"}).forEach(function (it) {
    count++;
    it.priorUse = "Summative";
    db.content.save(it);
  });
  print("Updated " + count + " records");
}
