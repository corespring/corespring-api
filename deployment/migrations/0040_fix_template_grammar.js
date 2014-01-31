
var simpleDragAndDropLabel = "Drag and Drop - Simple";
var complexDragAndDropLabel = "Drag and Drop - Complex";

function getXml(templateLabel) {
    return db.templates.findOne({"label": templateLabel}).xmlData;
}

function updateXml(templateLabel, xml) {
    db.templates.update({"label": templateLabel}, { "$set": {"xmlData": xml }});
}

function up() {

    updateXml(simpleDragAndDropLabel, getXml(simpleDragAndDropLabel)
        .replace("in on the", "into the"));

    updateXml(complexDragAndDropLabel, getXml(complexDragAndDropLabel)
        .replace("cells at teh end", "cells at the end")
        .replace("end of teach week", "end of each week"));

}

function down() {
    // nope.
}