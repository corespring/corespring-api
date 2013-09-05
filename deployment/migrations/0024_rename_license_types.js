function up(){
    var removeWhitespace = function(str){
        return str.replace(" ","-");
    }
    //find licenseType that exists and has a string value and replace license types with spaces with hyphens
    db.content.find(
        {"contributorDetails.licenseType": {"$exists": true}, "contributorDetails.licenseType":{"$type":2}},
        {"contributorDetails.licenseType": 1}
    ).forEach(function(item){
        var newLicenseType = removeWhitespace(item.contributorDetails.licenseType)
        db.content.update({"_id": item._id}, {"$set":{"contributorDetails.licenseType":newLicenseType}})
    })
}
function down(){
    //can't restore spaces because we don't know where the spaces were
}