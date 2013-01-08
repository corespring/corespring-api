var gradeLevel = {"$in": ["02", "03", "04", "05", "06"]};

function titleQuery(t){
  return  {"$regex": "\\b" + t, "$options": "i"};
}

function makeQuery(title, collectionIds) {
  return {
    "$or": [ {"title": titleQuery(title) } ],
    "gradeLevel": gradeLevel,
    "collectionId": {"$in": collectionIds }
  };
}

//505777f5e4b05f7845735bc1
var allIds = ["4ff2e56fe4b077b9e3168a05","5001b9b9e4b035d491c268c3","5001bb0ee4b0d7c9ec3210a2","5021814ce4b03e00504e4741","505777f5e4b05f7845735bc1","5072e73b1c00df6fdd627594","50a22ccc300479fa2a5a66ac"];
var queryOne = makeQuery("life", ["505777f5e4b05f7845735bc1"]);
var queryTwo = makeQuery("life", allIds);

printjson(queryOne);
db.content.find(queryOne, {_id:1, title:1}).forEach(printjson);
print("...")
db.content.find(queryTwo, {_id:1, title: 1}).forEach(printjson);
