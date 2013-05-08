var mapping = {
  "New England Common Assessments Program": "New England Common Assessment Program",
  "Ilustrative Mathematics": "Illustrative Mathematics",
  "Learn Zillion": "LearnZillion",
  "NNew York State Education Department": "New York State Education Department",
  "The College Board": "College Board"
};

function up() {
  db.content.find().forEach(function(item) {
    if (!item.contributorDetails) return;
    var changed = false;
    for (var badKey in mapping) {
      if (item.contributorDetails.contributor == badKey) {
        item.contributorDetails.contributor = mapping[badKey];
        print("contributor for "+item._id+": "+badKey+"->"+mapping[badKey]);
        changed = true;
      }

      if (item.contributorDetails.author == badKey) {
        item.contributorDetails.author = mapping[badKey];
        print("author for "+item._id+": "+badKey+"->"+mapping[badKey]);
        changed = true;
      }

      if (item.contributorDetails.copyright && item.contributorDetails.copyright.owner == badKey) {
        item.contributorDetails.copyright.owner = mapping[badKey];
        print("owner for "+item._id+": "+badKey+"->"+mapping[badKey]);
        changed = true;
      }
    }
    if (changed)
      db.content.save(item);
  });
}

function down() {
  // One way
}