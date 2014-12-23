var mathPracticeStandards = [
        {"_id":ObjectId("549967c968d7a3c7f606c7de"),"category":"Standards for Mathematical Practice » Make sense of problems and persevere in solving them.","dotNotation":"Practice.MP1","guid":"FBCBB7C696FE475695920CA622B1C854","standard":"Make sense of problems and persevere in solving them.","subCategory":"","subject":"Math","uri":"http://corestandards.org/Math/Practice/MP1","source":"manual","grades":["K","01","02","03","04","05","06","07","08","09","10","11","12"]}
        ,
        {"_id":ObjectId("549967cc68d7a3c7f606c7df"),"category":"Standards for Mathematical Practice » Reason abstractly and quantitatively.","dotNotation":"Practice.MP2","guid":"FBCBB7C696FE475695920CA622B1C855","standard":"Reason abstractly and quantitatively.","subCategory":"","subject":"Math","uri":"http://corestandards.org/Math/Practice/MP2","source":"manual","grades":["K","01","02","03","04","05","06","07","08","09","10","11","12"]}
        ,
        {"_id":ObjectId("549967ce68d7a3c7f606c7e0"),"category":"Standards for Mathematical Practice » Construct viable arguments and critique the reasoning of others.","dotNotation":"Practice.MP3","guid":"FBCBB7C696FE475695920CA622B1C856","standard":"Construct viable arguments and critique the reasoning of others.","subCategory":"","subject":"Math","uri":"http://corestandards.org/Math/Practice/MP3","source":"manual","grades":["K","01","02","03","04","05","06","07","08","09","10","11","12"]}
        ,
        {"_id":ObjectId("549967d068d7a3c7f606c7e1"),"category":"Standards for Mathematical Practice » Model with mathematics.","dotNotation":"Practice.MP4","guid":"FBCBB7C696FE475695920CA622B1C857","standard":"Model with mathematics.","subCategory":"","subject":"Math","uri":"http://corestandards.org/Math/Practice/MP4","source":"manual","grades":["K","01","02","03","04","05","06","07","08","09","10","11","12"]}
        ,
        {"_id":ObjectId("549967d168d7a3c7f606c7e2"),"category":"Standards for Mathematical Practice » Use appropriate tools strategically.","dotNotation":"Practice.MP5","guid":"FBCBB7C696FE475695920CA622B1C858","standard":"Use appropriate tools strategically.","subCategory":"","subject":"Math","uri":"http://corestandards.org/Math/Practice/MP5","source":"manual","grades":["K","01","02","03","04","05","06","07","08","09","10","11","12"]}
        ,
        {"_id":ObjectId("549967d268d7a3c7f606c7e3"),"category":"Standards for Mathematical Practice » Attend to precision.","dotNotation":"Practice.MP6","guid":"FBCBB7C696FE475695920CA622B1C859","standard":"Attend to precision.","subCategory":"","subject":"Math","uri":"http://corestandards.org/Math/Practice/MP6","source":"manual","grades":["K","01","02","03","04","05","06","07","08","09","10","11","12"]}
        ,
        {"_id":ObjectId("549967d468d7a3c7f606c7e4"),"category":"Standards for Mathematical Practice » Look for and make use of structure.","dotNotation":"Practice.MP7","guid":"FBCBB7C696FE475695920CA622B1C85A","standard":"Look for and make use of structure.","subCategory":"","subject":"Math","uri":"http://corestandards.org/Math/Practice/MP7","source":"manual","grades":["K","01","02","03","04","05","06","07","08","09","10","11","12"]}
        ,
        {"_id":ObjectId("5499698e68d7a3c7f606c7e5"),"category":"Standards for Mathematical Practice » Look for and express regularity in repeated reasoning.","dotNotation":"Practice.MP8","guid":"FBCBB7C696FE475695920CA622B1C85B","standard":"Look for and express regularity in repeated reasoning.","subCategory":"","subject":"Math","uri":"http://corestandards.org/Math/Practice/MP8","source":"manual","grades":["K","01","02","03","04","05","06","07","08","09","10","11","12"]}
    ];

function up() {
    print("adding math practice standards");
    db.ccstandards.insert(mathPracticeStandards);
}

function down() {
    db.ccstandards.remove({dotNotation: /Practice\.MP/})
}
