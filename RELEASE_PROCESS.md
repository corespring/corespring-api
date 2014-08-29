# Release process

This documents our current release process.


## release corespring-container 

    http://23.92.16.92:8080/job/container-release/
    
## merge master -> release branch 

    git checkout release
    git merge master
    
## update container version in `Dependencies.scala`

     val containerVersion = "0.X.X-SNAPSHOT" -> "0.X.X"
     
## local build, local test, local integration test

## push release

    git push origin release
    
## check ci tests/builds

Jenkins will run it's own tests + builds make sure they all pass.

## release notes


    
    
