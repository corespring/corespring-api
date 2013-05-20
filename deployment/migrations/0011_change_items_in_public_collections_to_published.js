function up(){
      var count = 0;
      //collections are ids for Mathematics, ELA, and Beta
      db.content.find({"collectionId": {"$in": ["4ff2e4cae4b077b9e31689fd","4ff2e56fe4b077b9e3168a05","505777f5e4b05f7845735bc1"]}}).forEach(function (it) {
        count++;
        it.published = true;
        db.content.save(it);
      });
      print("Updated " + count + " records");
}

function down(){
      var count = 0;
      //collections are ids for Mathematics, ELA, and Beta
      db.content.find({"collectionId": {"$in": ["4ff2e4cae4b077b9e31689fd","4ff2e56fe4b077b9e3168a05","505777f5e4b05f7845735bc1"]}}).forEach(function (it) {
        count++;
        it.published = false;
        db.content.save(it);
      });
      print("Updated " + count + " records");
}