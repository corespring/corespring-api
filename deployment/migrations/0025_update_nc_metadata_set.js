function up(){
    db.metadataSets.update(
        {"metadataKey":"new_classrooms"},
        {"$set":{"editorUrl":"http://metadata-form-ui-v2.herokuapp.com/newclassrooms"}, "$push":{"schema":{"key":"credits"}}}
    )
}
function down(){
    db.metadataSets.update(
        {"metadataKey":"new_classrooms"},
        {"$set":{"editorUrl":"https://metadata-form-ui-example.herokuapp.com/newclassrooms"}, "$pull":{"schema":{"key":"credits"}}}
    )
}