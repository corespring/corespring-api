function up() {
  var count = 0;
  db.content.find({"contributorDetails.contributor": "New England Common Assessment Program"}).forEach(function (it) {
    count++;
    it.contributorDetails.sourceUrl = "http://www.ride.ri.gov/InstructionAssessment/Assessment/NECAPAssessment/NECAPReleasedItems/tabid/426/LiveAccID/15410/Default.aspx";
    db.content.save(it);
  });
  print("Updated " + count + " records");
}

function down() {
  var count = 0;
  db.content.find({"contributorDetails.contributor": "New England Common Assessment Program"}).forEach(function (it) {
    count++;
    it.contributorDetails.sourceUrl = "www.ride.ri.gov/assessment/necap.aspx";
    db.content.save(it);
  });
  print("Updated " + count + " records");
}
