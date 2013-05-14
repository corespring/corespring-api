function up() {

  var update =  {
    $push: { contentcolls: { collectionId: ObjectId("5162d9b9ac1f68b4461138d9"), pval: NumberLong(3) } }
  };

  db.orgs.update({ name : "LearnZillion"}, update);
}

function down() {
  // Non-reversible
}