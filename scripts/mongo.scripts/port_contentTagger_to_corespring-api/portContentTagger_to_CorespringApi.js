if (!from || !to) {
    throw "You must specify from and to eg: --eval 'var from = \"dbone\"; var to = \"dbtwo\";'"
}

if(!d){
  throw "You also need so specify d = a Date object"
}
print("from db: " + from);
print("to db: " + to);
print("date: " + d);

conn = new Mongo("localhost");
corespringLiveDb = conn.getDB(from);

print(corespringLiveDb);

var dateNumber = d.getTime() + (d.getTimezoneOffset() * 60000);
print(d);
print(dateNumber);

var query = {dateModified: { $gte: dateNumber }};

print(">>> --- count: " + corespringLiveDb.mcas3.count(query));


var liveItems = corespringLiveDb.mcas3.find(query);

var apiDevDb = conn.getDB(to);

var suffixToContentTypeMap = {
    "jpg":"image/jpg",
    "jpeg":"image/jpg",
    "png":"image/png",
    "gif":"image/gif",
    "doc":"application/msword",
    "docx":"application/msword",
    "pdf":"application/pdf",
    "xml":"text/xml",
    "css":"text/css",
    "html":"text/html",
    "txt":"text/txt",
    "js":"text/javascript"
};

function getContentType(filename) {
    var split = filename.split(".");
    var suffix = split[split.length - 1];
    if(!suffix || !suffixToContentTypeMap[suffix.toLowerCase()]){
        print( "can't find filename for: " + filename);
        return "unknown";
    }
    return suffixToContentTypeMap[suffix.toLowerCase()];
}

function collection_to_collectionId(fromItem, toItem) {
    var collectionName = fromItem.collection;

    if (!collectionName) {
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

        var contentType = getContentType(file.filename);

        if( contentType == "unknown"){
            print("not adding file: " + file.filename);
        }
        else {
            targetItem.data.files.push(
                {
                    _t:"models.StoredFile",
                    name:file.filename,
                    isMain:false,
                    storageKey:fromItem._id + "/" + file.filename,
                    contentType:getContentType(file.filename)
                }
            );
        }
    }

}

function subjects_obj_to_id(from, to) {

    if (!from.primarySubject && !from.relatedSubject) {
        return;
    }

    to.subjects = {};

    if (from.primarySubject && from.primarySubject.refId) {
        to.subjects.primary = ObjectId(from.primarySubject.refId);
    }

    if (from.relatedSubject && from.relatedSubject.refId) {
        to.subjects.related = ObjectId(from.relatedSubject.refId);
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

function itemType(from, to) {
    if (!from.itemType) {
        return;
    }

    if (from.itemType == "Other") {
        to.itemType = from.itemTypeOther;
    } else {
        to.itemType = from.itemType;
    }
}


function copyright(from, to){

    if(!from.copyrightOwner && !from.copyrightYear && !from.copyrightExpirationDate){
        return;
    }

    to.copyright = {};

    if( from.copyrightOwner){
        to.copyright.owner = from.copyrightOwner;
        switch (from.copyrightOwner) {
            case "New York State Education Department":
                to.copyright.imageName = "nysed.png";
                break;
            case "State of New Jersey Department of Education":
                to.copyright.imageName = "njded.png";
                break;
            case "Illustrative Mathematics":
                to.copyright.imageName = "illustrativemathematics.png";
                break;
            case "Aspire Public Schools":
                to.copyright.imageName = "aspire.png";
                break;
        }
    }


    if( from.copyrightYear){
        to.copyright.year= from.copyrightYear;
    }

    if( from.copyrightExpirationDate){
        to.copyright.expirationDate = from.copyrightExpirationDate;
    }
}

function contributorDetails(from, to){

    to.contributorDetails = {};
    var details = to.contributorDetails;
    details.contributor = from.contributor;
    details.credentials = from.credentials;
    details.author = from.author;
    details.sourceUrl = from.sourceUrl;
    details.licenseType = from.licenseType;
    details.costForResource = from.costForResource;

    copyright(from, details);
}


var ignoredProperties = ["files",
    "xmlData",
    "_id",
    "primarySubject",
    "relatedSubject",
    "standards",
    "collection",
    "_typeHint",
    "itemType",
    "itemTypeOther",
    "contributor",
    "credentials",
    "copyrightOwner",
    "copyrightYear",
    "copyrightExpirationDate",
    "author",
    "sourceUrl",
    "licenseType",
    "costForResource",
    "primaryStandard"];

function convertLiveItemToApiItem(item) {
    //print(item.title);
    var target = {};
    target._id = item._id;

    xmlData_to_resource(item, target);
    collection_to_collectionId(item, target);
    subjects_obj_to_id(item, target);
    standards_obj_to_id(item, target);
    itemType(item, target);
    contributorDetails(item, target);

    for (var x in item) {

        //print("copying: " + x);

        if (ignoredProperties.indexOf(x) == -1) {
            target[x] = item[x];
            //print("now: " + x + ": " + target[x])
        } else {
            //print("ignoring");
        }
    }
    print("inserting: ");
    print(target.title);
    apiDevDb["content"].save(target);
}


liveItems.forEach(convertLiveItemToApiItem);

