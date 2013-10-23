function up(){

    var NEW_CLASSROOMS_COLLECTION_ID = "51df104fe4b073dbbb1c84fa";

    /**
     * Removes an existing shuffle="true/false" attribute from a <choiceInteraction/> node.
     */
    function stripExistingShuffleAttribute(startTag) {
        var choiceInteractionStartTagRegex = /<choiceInteraction([.\s\S]*)(shuffle=\".*?\")([.\s\S]*)([>|\>])/gm;
        var match = choiceInteractionStartTagRegex.exec(startTag);
        if (match !== null) {
            return startTag.replace(choiceInteractionStartTagRegex, "<choiceInteraction$1$3$4");
        } else {
            return startTag;
        }
    }

    /**
     * Removes existing shuffle="true/false" attribute from a <choiceInteraction/>, replacing it with shuffle="true".
     */
    function processStartTag(startTag) {
        var stripped = stripExistingShuffleAttribute(startTag);
        var test = "<choiceInteraction$1" + " shuffle=\"true\"" + "$2";
        return stripped.replace(/<choiceInteraction([.\s\S]*?)([>|/>])/gm, test).replace(/  /g, " ");
    }

    /**
     * Ensures shuffle="true" for all <choiceInteraction/> nodes.
     */
    function rewrite(item) {
        var replacementItem = item;
        var choiceInteractionRegex = new RegExp(/<choiceInteraction([.\s\S]*?)[>|/>]/gm);
        var result;
        while ((startTag = choiceInteractionRegex.exec(item)) !== null) {
            replacementItem = replacementItem.replace(startTag[0], processStartTag(startTag[0]));
        }
        return replacementItem;
    }

    var newClassroomsContent = db.content.find({"collectionId": NEW_CLASSROOMS_COLLECTION_ID});

    newClassroomsContent.forEach(function(content) {
        content.data.files.forEach(function(file) {
            if (file.isMain && file.content) {
                file.content = rewrite(file.content);
            }
        });
        db.content.save(content);
    });

}