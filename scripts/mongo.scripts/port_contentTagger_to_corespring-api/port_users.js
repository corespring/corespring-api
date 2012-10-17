//expects vars from and to to have been set in an eval
if( !from || !to ){
    throw "You must specify from and to eg: --eval 'var from = \"dbone\"; var to = \"dbtwo\";'"
}
print("from db: " + from);
print("to db: " + to);

conn = new Mongo("localhost");
var fromDb = conn.getDB(from);
var users = fromDb.users.find();
var toDb = conn.getDB(to);


var org = { "name" : "Root Org" ,
    "path" : [
        ObjectId("502404dd0364dc35bb393398")
    ],
    "contentcolls" : [
        //{ "collectionId" : { "$oid" : "5001bb0ee4b0d7c9ec3210a2"} , "pval" : 9223372036854775807} ,
    ] ,
    "_id" : ObjectId( "502404dd0364dc35bb393398" )
    };

print(">>>> toDb " + toDb.contentcolls.count());

toDb.contentcolls.find().forEach( function(collection){
    org.contentcolls.push( { collectionId: collection._id});
});

//org.contentcolls.push( { collectionId: mcas3Collection._id });

toDb.orgs.insert(org);

print( "new org id : " + org._id );

var accessToken = {
    "organization" : org._id ,
    "tokenId" : "34dj45a769j4e1c0h4wb",
    "creationDate" : ISODate("2012-09-03T18:47:33.087Z"),
    "expirationDate" : ISODate("2013-09-09T18:47:33.087Z")
};


toDb.accessTokens.insert(accessToken);


users.forEach(function(user){

   var newUser = {};
    newUser._id = user._id;
    newUser.userName = user.username;
    newUser.fullName = user.realname;
    newUser.email = "TODO";
    newUser.orgs = [
        {
            "orgId": ObjectId("502404dd0364dc35bb393398"),
            "pval": NumberLong("9223372036854775807")
        }
    ];

    toDb.users.insert(newUser);
});
