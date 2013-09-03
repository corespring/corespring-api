var up = (function(){
  
  var NEW_CLASSROOMS_COLLECTION_ID = "51df104fe4b073dbbb1c84fa";
  var RESPONSE_DECLARATION_REGEX = /<responseDeclaration.*(\n?)(.*)\>/g;
  var EXACT_MATCH_ATTRIBUTE = "exactMatch=\"false\"";

  function rewrite(item) {
    return item.replace(EXACT_MATCH_ATTRIBUTE, "").replace(RESPONSE_DECLARATION_REGEX, "<responseDeclaration $1$2 " + EXACT_MATCH_ATTRIBUTE + ">")
  }

  return function(){
    var newClassroomsContent = db.content.find({"collectionId": NEW_CLASSROOMS_COLLECTION_ID});

    newClassroomsContent.forEach(function(content) {
      content.data.files.forEach(function(file) {
        if (file.isMain && file.content) {
          file.content = rewrite(file.content);
        }
      });
      db.content.save(content);
    });
  };
  
})();