# update_mongo_document_from_corespring_content.rb
# updates a mongo document in a db with data stored locally in a corespring-content repo
# example:
#  ruby update_mongo_document_from_corespring_content.rb 503fd699e4b02288e5f0ebd0 tdb ../../corespring-content
#eg: ruby upload_file_to_amazon.rb AKIAJNPUNTVH2HMFWVVA sl+sXsuq8Xkbl4NvlLuyHRZtrVJp+BXEoH7XlLPm corespring-assets /Users/edeustace/Desktop/cute-puppy.jpg test_images

require 'rubygems'
require 'mongo'
require 'aws/s3'
require_relative 'lib/amazon_uploader'
require_relative 'lib/corespring_content_uploader'
require 'fileutils'


def beta_items(db_name, collection_name)
  connection = Mongo::Connection.new("localhost", 27017)
  db = connection.db(db_name)
  collection_id = get_collection_id( collection_name, db)
  content_coll = db.collection("content")
  content_coll.find( { "collectionId" => collection_id}).to_a
end


def get_collection_id( collection_name, db )
  collection_coll = db.collection("contentcolls")
  puts collection_coll.count
  found_collections = collection_coll.find({"name" => collection_name}, {}).to_a
  c = found_collections[0]
  puts c
  raise "Can't find collection with name: #{collection_name}" if c.nil?
  c["_id"].to_s
end



db_name = "tdb"
output_path = "../conf/test-data/exemplar-content"
collection_name = "Beta Items"

FileUtils.rm_rf("#{output_path}/.", secure: true)

items = []
#items << { "item_id" => "503fd699e4b02288e5f0ebd0", "path" =>  "../../corespring-content" }
#items << { "item_id" => "5040d048e4b0d43b60f3a00f", "path" =>  "../../corespring-content/Items for Ed" }
#items << { "item_id" => "5040da52e4b0d43b60f3a011", "path" =>  "../../corespring-content/Items for Ed" }
#items << { "item_id" => "5044cf96e4b008d30cf773a4", "path" => "../../corespring-content/Items for Ed" }

beta_items( db_name, collection_name ).each do |bi| 
    items << { "item_id" => bi["_id"].to_s, "path" => "../../corespring-content" }
end


puts items

uploader = AmazonUploader.new( "AKIAJNPUNTVH2HMFWVVA", "sl+sXsuq8Xkbl4NvlLuyHRZtrVJp+BXEoH7XlLPm", "corespring-assets")

items.each do |i|
  updater = CorespringContentUpdater.new(i["item_id"], db_name, i["path"], uploader, output_path)
  updater.begin
end


