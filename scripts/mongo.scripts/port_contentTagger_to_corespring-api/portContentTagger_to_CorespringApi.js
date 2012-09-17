if (!from || !to) {
    throw "You must specify from and to eg: --eval 'var from = \"dbone\"; var to = \"dbtwo\";'"
}
print("from db: " + from);
print("to db: " + to);

conn = new Mongo("localhost");
corespringLiveDb = conn.getDB(from);

print(corespringLiveDb)

//var query = { "title": /.*car.*/};
var query = {};
print(">>> --- count: " + corespringLiveDb.mcas3.count(query));

var liveItems = corespringLiveDb.mcas3.find(query);
print(liveItems);

var apiDevDb = conn.getDB(to);

var suffixToContentTypeMap = {
    "jpg" : "image/jpg",
    "jpeg" : "image/jpg",
    "png" : "image/png",
    "gif" : "image/gif",
    "doc" : "application/msword",
    "docx" : "application/msword",
    "pdf" : "application/pdf",
    "xml" : "text/xml",
    "css" : "text/css",
    "html" : "text/html",
    "txt" : "text/txt",
    "js" : "text/javascript"
};

function getContentType(filename) {
    var split = filename.split(".");
    var suffix = split[split.length - 1];
    return suffixToContentTypeMap[suffix];
}

function collection_to_collectionId(fromItem, toItem) {
    var collectionName = fromItem.collection;

    print(">>>> -- " + collectionName);

    if(!collectionName){
        return;
    }

    apiDevDb.contentcolls.find({ name:collectionName }).forEach(function (c) {
        toItem.collectionId = "" + c._id;
        //print("toItem.collectionId: " + toItem.collectionId);
    });
}

function xmlData_to_resource(fromItem, targetItem) {

    targetItem.data = {};
    targetItem.data.name = "data";
    targetItem.data.files = [];
    var dataFile = {
        _t:"models.VirtualFile",
        name:"qti.xml",
        contentType:"text/xml",
        isMain:true,
        content:fromItem.xmlData };

    targetItem.data.files.push(dataFile);

    for (var x in fromItem.files) {
        var file = fromItem.files[x];

        targetItem.data.files.push(
            {
                _t: "models.StoredFile",
                name:file.filename,
                isMain:false,
                storageKey: fromItem._id + "/" + file.filename,
                contentType:getContentType(file.filename)
            }
        );
    }

}

function primarySubject_obj_to_id(from, to) {
    if (from.primarySubject) {
        if(from.primarySubject.refId){
          to.primarySubject = ObjectId(from.primarySubject.refId);
        }
    }
}

function standards_obj_to_id(from, to) {
    if (from.standards) {
        to.standards = [];
        for (var i = 0; i < from.standards.length; i++) {
            to.standards.push(ObjectId(from.standards[i].refId));
        }
    }
}

var ignoredProperties = ["files",
    "xmlData",
    "_id",
    "primarySubject",
    "standards",
    "collection",
    "_typeHint",
    "primaryStandard"];

function convertLiveItemToApiItem(item) {
    print(item.title);
    var target = {};
    target._id = item._id;

    xmlData_to_resource(item, target);
    collection_to_collectionId(item, target);
    primarySubject_obj_to_id(item, target);
    standards_obj_to_id(item, target);

    for (var x in item) {

        //print("copying: " + x);

        if (ignoredProperties.indexOf(x) == -1) {
            target[x] = item[x];
            //print("now: " + x + ": " + target[x])
        } else {
            //print("ignoring");
        }
    }
    apiDevDb["content"].insert(target);
}


liveItems.forEach(convertLiveItemToApiItem);

