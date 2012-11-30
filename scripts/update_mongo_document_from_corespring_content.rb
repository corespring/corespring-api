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

def all_items(db_name)
  connection = Mongo::Connection.new("localhost", 27017)
  db = connection.db(db_name)
  content_coll = db.collection("content")
  content_coll.find().to_a
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



db_name = "cs-api-test"
#db_name = "cs-api-test"
output_path = "../conf/seed-data/exemplar-content/content"
collection_name = "Beta Items"

FileUtils.rm_rf("#{output_path}/.", secure: true)

items = []

#
# NOTE: When doing the full import - run with all_items not just beta_items
#
#all_items( db_name ).each do |bi|
beta_items(db_name, collection_name ).each do |bi|
    items << { "item_id" => bi["_id"].to_s, "path" => "../../corespring-content" }
end


puts "Found #{items.length} items to update"

uploader = AmazonUploader.new( "AKIAJNPUNTVH2HMFWVVA", "sl+sXsuq8Xkbl4NvlLuyHRZtrVJp+BXEoH7XlLPm", "corespring-assets")

generate_json = true 

items.each do |i|
  updater = CorespringContentUpdater.new(i["item_id"], db_name, i["path"], uploader, output_path, generate_json)
  updater.begin
end


