# Seed Data

This folder contains seed data for the application.

When the application starts it adds this data to the db.

Depending on which mode you run in it'll insert different data.

The seed data can be of 3 formats:

# Single file where each line is a document in a collection and the file name matches a collection name.

eg: common/orgs.json  => will insert each line of the file into the 'orgs' collection

# Single file whose name is list.json and whose contents is a json array

eg: common/subject/list.json  => will insert each item in the array into the 'subject' collection

# Folder that contains x number of json files (and doesn't contain 'list.json')

eg: dev/content/*.json => will insert each json file into the 'content' collection.

