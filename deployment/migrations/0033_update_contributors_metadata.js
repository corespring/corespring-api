//update nj dept of ed, nys dept of ed License type to CC BY, Prior Use to NONE (no selection) and Grade level #1 should match grade level #2
//update kentucky dept of ed License to CC BY and Grade level #1 should match grade level #2
function up(){
    db.content.find({"$or":[
        {"contributorDetails.contributor":"State of New Jersey Department of Education"},
        {"contributorDetails.contributor":"New York State Education Department"},
        {"contributorDetails.contributor":"Kentucky Department of Education"}
    ]}).forEach(function(item){
        item.contributorDetails.licenseType = "CC BY"
        item.priorGradeLevel = item.taskInfo.gradeLevel
        if(item.contributorDetails.contributor != "Kentucky Department of Education"){
            delete item.priorUse
        }
        db.content.save(item)
    })
}
function down(){
    //we don't record what the previous values were, hence no rollback
}