function up() {
    var NEW_CLASSROOMS_COLLECTION_ID = "51df104fe4b073dbbb1c84fa";

    var newClassroomsContent = db.content.find({"collectionId": NEW_CLASSROOMS_COLLECTION_ID});

    function isMultipleChoice(content) {
        return content.taskInfo && content.taskInfo.itemType === "Multiple Choice";
    }

    function removeChoiceStyle(item) {
        var choiceInteractionRegex = /<choiceInteraction([.\s\S]*?)>/;
        var match = choiceInteractionRegex.exec(item);
        var newText = (match == null ? null : match[1].replace('choiceStyle="rightAlign"', ''));
        if (newText != null) {
            return item.replace(match[1], newText);
        } else {
            return item;
        }
    }

    newClassroomsContent.forEach(function(content) {
        if (isMultipleChoice(content)) {
            content.data.files.forEach(function(file) {
                if (file.isMain && file.content) {
                    file.content = removeChoiceStyle(file.content);
                    db.content.save(content);
                }
            });
        }
    });
}