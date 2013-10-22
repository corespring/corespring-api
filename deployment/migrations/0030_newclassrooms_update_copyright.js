var up = (function(){

    var NEW_CLASSROOMS_COLLECTION_ID = "51df104fe4b073dbbb1c84fa";

    var newClassroomsContent = db.content.find({"collectionId": NEW_CLASSROOMS_COLLECTION_ID});

    newClassroomsContent.forEach(function(content) {
        content.contributorDetails.copyright.owner = "New Classrooms Innovation Partners";
        db.content.save(content);
    });

})();