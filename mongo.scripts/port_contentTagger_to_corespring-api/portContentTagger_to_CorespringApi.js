conn = new Mongo("localhost");
corespringLiveDb = conn.getDB("corespring-live");

var liveItems = corespringLiveDb.mcas3.find({ title: /.*cars.*/ });


var apiDevDb = conn.getDB("corespring-api-dev");

var itemCount = 0;

function getContentType(filename){
    var split = filename.split(".");
    return split[split.length - 1];
}

function xmlData_to_resource(fromItem, targetItem){

    targetItem.data = {};
    targetItem.data.name = "data";
    targetItem.data.files = [];
    var dataFile = {
        _t : "common.models.VirtualFile",
        name: "qti.xml",
        contentType : "xml",
        default : true,
        content: fromItem.xmlData };

    targetItem.data.files.push(dataFile);

    for ( var x in fromItem.files ){
        var file = fromItem.files[x];

        targetItem.data.files.push(
            {
                name: file.filename,
                default: false,
                storageKey: "blah",
                contentType: getContentType(file.filename)
            }
        );
    }

}

function convertLiveItemToApiItem(item){
    var target = {};
    xmlData_to_resource(item,target);
    apiDevDb["content"].insert(target);
}


liveItems.forEach( convertLiveItemToApiItem );

