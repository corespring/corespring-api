var suffixToContentTypes = {
    png: "image/png",
    docx: "application/msword"
}

var updateFile = function(f){
    if(f.contentType != "unknown"){
        return false;
    }

    var changed = false;

    var lastIndex = f.name.lastIndexOf(".");

    if(lastIndex != -1){
        var suffix = f.name.substring(lastIndex + 1, f.name.length);
        print(f.name);
        print(suffix);

        var lowercaseSuffix = suffix.toLowerCase();

        var mappedType = suffixToContentTypes[lowercaseSuffix];

        if(mappedType){
            print("changed == true for:" + f.name);
            f.contentType = mappedType;
            changed = true;
        } else {
            print("Warning: no mapped type for: " + lowercaseSuffix);
        }
    }
    return changed;
};

var updateResource = function(resource){
    var changed = false;
    for( var x = 0; x < resource.files.length; x++ ){
        var f = resource.files[x];
        changed = updateFile(f) || changed;
    }
    return changed;
};

var updateDataResource = function(item){
    var changed = updateResource(item.data);
    if(changed){
        print("Save item: " + item._id._id);
        db.content.save(item);
    }
};

var updateSupportingMaterials = function(item){

    var changed = false;

    for(var x = 0; x < item.supportingMaterials.length; x++){
        changed = updateResource(item.supportingMaterials[x]) || changed;
    }

    if(changed){
        print("Save item: " + item._id._id);
        db.content.save(item);
    }
}

function up(){
    db.content.find({"data.files.contentType" : "unknown"}).forEach(updateDataResource);
    db.content.find({"supportingMaterials.files.contentType" : "unknown"}).forEach(updateSupportingMaterials);
}

function down(){
  //One way migration
}



