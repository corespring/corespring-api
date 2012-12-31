Corespring API
==============


# Getting set up

Run MongoDB locally on the default port:

    $ mongod
    mongod --help for help and startup options
    Sun Sep  2 08:51:56 [initandlisten] MongoDB starting : pid=593 port=27017 dbpath=/data/db/ 64-bit host=bburton-macbook.local
    Sun Sep  2 08:51:56 [initandlisten] db version v2.0.6, pdfile version 4.5
    Sun Sep  2 08:51:56 [initandlisten] git version: e1c0cbc25863f6356aa4e31375add7bb49fb05bc
    Sun Sep  2 08:51:56 [initandlisten] build info: Darwin erh2.10gen.cc 9.8.0 Darwin Kernel Version 9.8.0: Wed Jul 15 16:55:01 PDT 2009; root:xnu-1228.15.4~1/RELEASE_I386 i386 BOOST_LIB_VERSION=1_40
    Sun Sep  2 08:51:56 [initandlisten] options: {}
    Sun Sep  2 08:51:56 [initandlisten] journal dir=/data/db/journal
    Sun Sep  2 08:51:56 [initandlisten] recover : no journal files present, no recovery needed
    Sun Sep  2 08:51:56 [websvr] admin web console waiting for connections on port 28017
    Sun Sep  2 08:51:56 [initandlisten] waiting for connections on port 27017


Run the application using the Play SBT run target:

    $ play
    
    [corespring-api] $ run
    
    --- (Running the application from SBT, auto-reloading is enabled) ---
    
    [info] play - Listening for HTTP on port 9000...

If you want, you can use the convenience script to test the API in bin/cscurl. I add it to my PATH environment variable by putting this in my ~/.bashrc:

    export CS_API_ROOT=/Users/bburton/Documents/workspace/corespring-api
    PATH=$PATH:$CS_API_ROOT/bin
		
At which point you can access the API like so:

    $cscurl api/v1/items/500d918fe4b0c748988e88ad
    {
        "author": "State Department of Education",
        "collectionId": "50218190e4b03e00504e4742",
        "contentType": "item",
        "contributor": "State Department of Education",
        "copyrightYear": "2011",
        "gradeLevel": [
            "11"
        ],
        "id": "500d918fe4b0c748988e88ad",
        "itemType": "Multiple Choice",
        "licenseType": "CC BY",
        "primarySubject": {
            "category": "Mathematics",
            "refId": "4ffb535f6bb41e469c0bf2c2",
            "subject": ""
        },
        "priorUse": "Summative",
        "sourceUrl": "http://statedoe.gov",
        "title": "Trevor has $6 to spend on raisins and peanuts. Raisins cost $1 per pound, and peanuts cost $2 per pound. Which graph shows the relationship between the number of pounds of raisins and the number of pounds of peanuts that Trevor can buy?"
    }

# Production Environment Variables

EMAIL_HOST:                 smtp.sendgrid.net

EMAIL_PASSWORD:             j4r1qjnm

EMAIL_USER:                 app8091788@heroku.com

ENV_INIT_DATA:              false

ENV_MONGO_URI:              mongodb://corespring:cccrcId4d@ds039017.mongolab.com:39017/corespring-app

GOOGLE_CLIENT_ID:           1091241109207.apps.googleusercontent.com

GOOGLE_CLIENT_SECRET:       MXuLdNojHZYY4ciVWndrNRml

SENDGRID_PASSWORD:          j4r1qjnm

SENDGRID_USERNAME:          app8091788@heroku.com



