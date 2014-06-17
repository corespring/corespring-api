# Corespring Container Regression Testing
 
## Prerequisites
[Node and Npm](http://nodejs.org/)

## Installing
    npm install 
 
## Running 
 
### Run tests locally
  *Note: make sure container is running and accessible under localhost:9000*  
  
    grunt regression

### Run functional tests against SauceLabs with a non-local base URL
  *Make sure SAUCE_USERNAME and SAUCE_ACCESS_KEY are set in your env*  
  
    grunt regression --local=false --baseUrl=http://corespring-app-qa.herokuapp.com/
   
     
     
   