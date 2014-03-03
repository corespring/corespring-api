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
        var correctAnswer = getCorrectAnswer(item);
        if (correctAnswer) {
          return "Good try, but " + correctAnswer + " is the correct answer.";
        } else {
          return undefined;
        }
    }

    /**
     * Given an item body, returns the first correct answer for that item.
     */
    function getCorrectAnswer(item) {
        var valueRegex = /<value>([.\s\S]*?)<\/value>/gm;
        var correctResponseRegex = /<correctResponse.*>([.\s\S]*?)<\/correctResponse>/gm;
        var match = correctResponseRegex.exec(item);
        var values = (match.length > 0) ? match[1] : undefined;
        var correctAnswerMatch = values ? valueRegex.exec(values) : undefined;

        if (correctAnswerMatch && correctAnswerMatch.length > 0) {
          return correctAnswerMatch[1].trim();
        } else {
          return undefined;
        }
    }

    function isShortAnswer(content) {
        return (content.taskInfo && content.taskInfo.itemType) ? content.taskInfo.itemType === "Constructed Response - Short Answer" : false;
    }

    newClassroomsContent.forEach(function(content) {

        content.data.files.forEach(function(file) {
            if ((file.isMain && file.content) && isShortAnswer(content)) {
                if (getIncorrectAnswerMessage(file.content)) {
                    file.content = file.content.replace(
                      getIncorrectFeedback(file.content), getIncorrectAnswerMessage(file.content));
                    db.content.save(content);
                }
            }
        });

    });

}

up();
