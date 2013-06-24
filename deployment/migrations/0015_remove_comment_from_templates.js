function up() {
  var count = 0;
  db.templates.find().forEach(function (it) {

    var textToFind = "<!-- This example adapted from the PET Handbook, copyright University of Cambridge ESOL Examinations -->";

    var position = it.xmlData.indexOf(textToFind);

    if (position >= 0) {
      count++;
      it.xmlData = it.xmlData.substring(0, position-1) + it.xmlData.substring(position + textToFind.length);
      db.templates.save(it);
    }
  });

  print("Updated " + count + " records");
}

function down() {
  //One way only
}