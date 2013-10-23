function up() {

    var NEW_CLASSROOMS_COLLECTION_ID = "51df104fe4b073dbbb1c84fa";

    var newClassroomsContent = db.content.find({"collectionId": NEW_CLASSROOMS_COLLECTION_ID});

    /**
     * Returns the <feedbackBlock/> contents with incorrectResponse="true" from the item.
     */
    function getIncorrectFeedback(item) {
        var incorrectFeedbackRegex = /<div class=['|\"]feedback-block-incorrect['|\"]>([.\s\S]*)<\/div>/;
        var match = incorrectFeedbackRegex.exec(item);
        return match == null ? null : match[1].trim();
    }

    function getIncorrectAnswerMessage(item) {
        return "Good try, but " + getCorrectAnswer(item) + " is the correct answer.";
    }

    /**
     * Given an item body, returns the first correct answer for that item.
     */
    function getCorrectAnswer(item) {
        var correctResponseRegex = /<correctResponse.*>([.\s\S]*?)<\/correctResponse>/gm;
        var match = correctResponseRegex.exec(item);
        var values = match[1];
        var valueRegex = /<value>([.\s\S]*?)<\/value>/gm;
        return valueRegex.exec(values)[1].trim();
    }

    function isShortAnswer(content) {
        return content.taskInfo.itemType === "Constructed Response - Short Answer";
    }

    newClassroomsContent.forEach(function(content) {

        content.data.files.forEach(function(file) {
            if ((file.isMain && file.content) && isShortAnswer(content)) {
                file.content = file.content.replace(
                    getIncorrectFeedback(file.content), getIncorrectAnswerMessage(file.content));
                db.content.save(content);
            }
        });

    });

}