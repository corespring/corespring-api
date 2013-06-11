function up() {
  var count = 0;
  db.content.find({"contributorDetails.licenseType": "CC BY"}).forEach(function (it) {
    count++;
    it.contributorDetails.licenseType = "CC BY-SA";
    db.content.save(it);
  });

  print("Updated " + count + " records");
}

function down() {
  //One way only
}
