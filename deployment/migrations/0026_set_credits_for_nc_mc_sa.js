function up(){
    db.content.update({
                       "taskInfo.itemType":{
                          "$in":[
                             "Multiple Choice",
                             "Constructed Response - Short Answer"
                          ]
                       },
                       "collectionId":{      //the collections belonging to new classrooms organization
                          "$in":[
                             "51df104fe4b073dbbb1c84fa",
                             "51df1971e4b06780286ef866",
                             "51f2af95e4b0c56079d651bb",
                             "521f5b02e4b01a74ab337af1"
                          ]
                       }
                    },
                    {"$set":
                        {"taskInfo.extended.new_classrooms.credits":"2"}
                    },
                    {"multi":true})
}
function down(){
    //this is a breaking change since we lose the original value of credits
}