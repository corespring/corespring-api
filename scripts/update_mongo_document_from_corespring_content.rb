# update_mongo_document_from_corespring_content.rb
# updates a mongo document in a db with data stored locally in a corespring-content repo 
# example: 
#  ruby update_mongo_document_from_corespring_content.rb 503fd699e4b02288e5f0ebd0 tdb ../../corespring-content

require 'rubygems'
require 'mongo'



=begin
 Args:
 - item_id
 - db_name
 - corespring_content_path  
=end

item_id = ARGV[0]
db_name = ARGV[1]
corespring_content_path = ARGV[2]


class CorespringContentUpdater
  
  CONTENT_COLLECTION = "content"


  SUFFIX_TO_CONTENT_TYPE_MAP = {
    "jpg" => "image/jpg",
    "jpeg" => "image/jpg",
    "png" => "image/png",
    "gif" => "image/gif",
    "doc" => "application/msword",
    "docx" => "application/msword",
    "pdf" => "application/pdf",
    "xml" => "text/xml",
    "css" => "text/css",
    "html" => "text/html",
    "txt" => "text/txt",
    "js" => "text/javascript"
  }
  
  def initialize(item_id, db_name, corespring_content_path)
    puts "!! update_mongo_document_from_corespring_content"
    puts "!! item_id: #{item_id}"
    puts "!! db_name: #{db_name}"
    puts "!! corespring_content_path: #{corespring_content_path}"
    @item_id = item_id
    @db_name = db_name
    @corespring_content_path = corespring_content_path
    @connection = Mongo::Connection.new("localhost", 27017)
    @db = @connection.db(@db_name)
  end

  def begin
    Dir["#{@corespring_content_path}/**/#{@item_id}"].each do |f|
      @item_path = f  
    end
    puts "Found path: #{@item_path}"

    Dir.entries(@item_path).each do |f|

      unless f == "." || f == ".."
        full_path = "#{@item_path}/#{f}"
        #puts "full_path: #{full_path}"
        if File.directory?(full_path)
          create_supporting_material_from_folder(full_path) 
        else
          create_supporting_material_from_file(full_path)
        end
      end
    end

  end

  def create_supporting_material_from_folder(file_path) 

    sm_name = File.basename(file_path)
    puts "sm_name: #{sm_name}"
    puts "create_supporting_material_from_folder: #{file_path}"
    coll = @db.collection(CONTENT_COLLECTION)
    puts coll.count
    puts "find item with id: #{@item_id}"
    items = coll.find({"_id" => BSON::ObjectId(@item_id)}, {}).to_a
    item = items[0]

    sm_array = item["supportingMaterials"] || []

     sm = { 
      "name" => sm_name,
        "files" => []
      }

    Dir.entries(file_path).each do |f|
      unless f == "." || f == ".."
       
       if can_be_virtual_file(f)
        sm["files"] << create_virtual_file( "#{file_path}/#{f}")
       else
        puts "upload the file..."
        sm["files"] << create_stored_file("#{file_path}/#{f}")
       end 
      end
    end

    sm_array << sm

    coll.update({"_id" => BSON::ObjectId(@item_id)}, item)
    puts "------ "
    puts coll.find({"_id" => BSON::ObjectId(@item_id)}, {}).to_a
  end

  def can_be_virtual_file(filename) 
    File.extname(filename) == ".html"
  end

  def content_type(file_path) 
    suffix = File.extname(file_path)

    type = SUFFIX_TO_CONTENT_TYPE_MAP[suffix.gsub(".", "")] 
    puts "#{suffix}: found: type: #{type}"
    type || "unknown"
  end

  def create_stored_file(file_path)
    out = {
      "_t" => "models.StoredFile",
      "name" => File.basename(file_path),
      "contentType" => content_type(file_path),
      "isMain" => false,
      "storageKey" => upload_file_and_return_storage_key(file_path)
     } 
     out
  end

  def upload_file_and_return_storage_key(file_path)
    #TODO...
    "blah"
  end

  def create_virtual_file(file_path)
   out = {

    "_t" => "models.VirtualFile",
    "name" => File.basename(file_path),
    "contentType" => content_type(file_path),
    "isMain" => true,
    "content" => get_content(file_path)
   } 
   out
  end

  def get_content(file_path)
    file = File.open(file_path, "rb")
    contents = file.read
    file.close
    contents
  end


  def create_supporting_material_from_file(file_path) 
    puts "create_supporting_material_from_file: #{file_path}"
  end

end


updater = CorespringContentUpdater.new(item_id, db_name, corespring_content_path)
updater.begin










