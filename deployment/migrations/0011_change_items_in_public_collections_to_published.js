//add published property to all items. certain collections will have published set to true. everything else is false (draft)
function up(){
      var count = 0;
      //collections are ids for Mathematics, ELA, and Beta
      var publishedColls = ["4ff2e4cae4b077b9e31689fd","4ff2e56fe4b077b9e3168a05","505777f5e4b05f7845735bc1"];
      db.content.find().forEach(function (it) {
        count++;
        if(publishedColls.indexOf(it.collectionId) != -1){
            it.published = true;
        }else{
            it.published = false;
        }
        db.content.save(it);
      });
      print("Updated " + count + " records");
}

function down(){
      var count = 0;
      //collections are ids for Mathematics, ELA, and Beta
      db.content.find().forEach(function (it) {
        count++;
        delete it['published'];
        db.content.save(it);
      });
      print("Updated " + count + " records");
}