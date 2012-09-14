# update_mongo_document_from_corespring_content.rb
# updates a mongo document in a db with data stored locally in a corespring-content repo
# example:
#  ruby update_mongo_document_from_corespring_content.rb 503fd699e4b02288e5f0ebd0 tdb ../../corespring-content
#eg: ruby upload_file_to_amazon.rb AKIAJNPUNTVH2HMFWVVA sl+sXsuq8Xkbl4NvlLuyHRZtrVJp+BXEoH7XlLPm corespring-assets /Users/edeustace/Desktop/cute-puppy.jpg test_images


require 'rubygems'
require 'mongo'
require 'aws/s3'


=begin
 Args:
 - item_id
 - db_name
 - corespring_content_path  
=end

item_id = ARGV[0]
db_name = ARGV[1]
corespring_content_path = ARGV[2]
output_path = ARGV[3]

class AmazonUploader

  def initialize(access_key, secret_key, bucket)
    AWS::S3::Base.establish_connection!(
      :access_key_id     => access_key,
      :secret_access_key => secret_key
    )
    @bucket = bucket
  end

  def store( file_path, s3_name )
    AWS::S3::S3Object.store( s3_name, open(file_path), @bucket)
    bucket = AWS::S3::Bucket.find(@bucket)
    result = bucket[s3_name]
    result
  end

end

class CorespringContentUpdater

  CONTENT_COLLECTION = "content"

  VIRTUAL_FILE_TYPES = ["html", "css", "js", "txt", "xml"]

  STORED_FILE_TYPES = ["jpg", "jpeg", "png", "gif", "pdf"]

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

  def initialize(item_id, db_name, corespring_content_path, uploader, output_path)
    puts "!! update_mongo_document_from_corespring_content"
    puts "!! item_id: #{item_id}"
    puts "!! db_name: #{db_name}"
    puts "!! corespring_content_path: #{corespring_content_path}"
    @item_id = item_id
    @db_name = db_name
    @corespring_content_path = corespring_content_path
    @connection = Mongo::Connection.new("localhost", 27017)
    @db = @connection.db(@db_name)
    @uploader = uploader
    @output_path = output_path 
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

  private

  def create_storage_key(sm_name, file_name)
    #50083ba9e4b071cb5ef79101/materials/Rubric/cute-kittens1.jpg
    "#{@item_id}/materials/#{sm_name}/#{File.basename(file_name)}"
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

    if sm_array.index{ |i| i["name"] == sm_name } != nil
      puts "Already exists - DON'T ADD"
    else
      new_sm = create_sm(sm_name, file_path)   
      sm_array << new_sm 
      item["supportingMaterials"] = sm_array
      coll.update({"_id" => BSON::ObjectId(@item_id)}, item)
    end
 
    puts "------ "
    #puts coll.find({"_id" => BSON::ObjectId(@item_id)}, {}).to_a
    export_json
  end

  def create_sm(name, file_path)

    sm = {
      "name" => name,
      "files" => []
    }

    Dir.entries(file_path).each do |f|
      unless f == "." || f == ".."

        new_file_path = "#{file_path}/#{f}"
        if can_be_virtual_file(f)
          sm["files"] << create_virtual_file( new_file_path)
        elsif can_be_stored_file(f)
          puts "upload the file...#{f}"
          storage_key = create_storage_key(name, new_file_path)
          sm["files"] << create_stored_file(storage_key, new_file_path)
        else
          puts "!!!Don't know how to handle file: #{file_path}/{f}"
        end
      end
    end

    sm
  
  end

  def export_json
    eval = "db.content.find({_id: ObjectId('#{@item_id}')}).forEach(printjson)"
    out = `mongo #{@db_name} --eval "#{eval}"`
    out = strip_object_id(out)
    out = remove_preamble(out)
    out.match(/"title".*?:.*?"(.*?\s+.*?\s+.*?)\s+/m)
    json_file_name = create_json_file_name(out)
    f = File.new("#{@output_path}/#{json_file_name}.json", "w")
    f.write(out)
    f.close
  end

  def create_json_file_name(mongo_json)
    
    name = "no-title"  
    match  = mongo_json.match(/"title".*?:.*?"(.*?\s+.*?\s+.*?)\s+/m)
    
    if !match.nil? && match.length == 2
      t = match[1]
      name = t.gsub(" ", "-")
    end

    "#{@item_id}-#{name}"
  end


  def strip_object_id(mongo_json)
    mongo_json.gsub(/ObjectId\((.*?)\)/, "{ \"$oid\" : \\1 }")
  end

  def remove_preamble(mongo_json)
    mongo_json.gsub(/\A.*?{/m, "{")
  end

  def can_be_virtual_file(filename)
    suffix = File.extname(filename).gsub(".", "")
    VIRTUAL_FILE_TYPES.include? suffix
  end

  def can_be_stored_file(filename)
    suffix = File.extname(filename).gsub(".", "")
    STORED_FILE_TYPES.include? suffix
  end

  def content_type(file_path)
    suffix = File.extname(file_path)

    type = SUFFIX_TO_CONTENT_TYPE_MAP[suffix.gsub(".", "")]
    puts "#{suffix}: found: type: #{type}"
    type || "unknown"
  end

  def create_stored_file(storage_key,file_path)

    result = @uploader.store(file_path, storage_key)
    puts "result: #{result}"

    out = {
      "_t" => "models.StoredFile",
      "name" => File.basename(file_path),
      "contentType" => content_type(file_path),
      "isMain" => false,
      "storageKey" => storage_key
    }
    out
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


uploader = AmazonUploader.new( "AKIAJNPUNTVH2HMFWVVA", "sl+sXsuq8Xkbl4NvlLuyHRZtrVJp+BXEoH7XlLPm", "corespring-assets")
updater = CorespringContentUpdater.new(item_id, db_name, corespring_content_path, uploader, output_path)
updater.begin
