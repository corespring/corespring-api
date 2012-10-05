#!ruby19
# encoding: utf-8

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
    puts "update_mongo_document_from_corespring_content, item_id: #{item_id}, db_name: #{db_name}, corespring_content_path: #{corespring_content_path}"
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

    if @item_path.nil?
      puts " Can't find any items with that id: #{@item_id} just export the json"
      export_json
      return 
    end

    Dir.entries(@item_path).each do |f|

      unless f == "." || f == ".."
        full_path = "#{@item_path}/#{f}"
        create_supporting_material(full_path)
      end
    end
  end

  private

  def create_storage_key(sm_name, file_name)
    "#{@item_id}/materials/#{sm_name}/#{File.basename(file_name)}"
  end

  def create_supporting_material(file_path)

    sm_name = File.basename(file_path)
    puts "sm_name: #{sm_name} create_supporting_material: #{file_path}"
    coll = @db.collection(CONTENT_COLLECTION)
    items = coll.find({"_id" => BSON::ObjectId(@item_id)}, {}).to_a
    item = items[0]

    sm_array = item["supportingMaterials"] || []

    if sm_array.index{ |i| i["name"] == sm_name } != nil
      puts "Already exists - DON'T ADD: #{sm_name}"
    else
      new_sm = create_sm(sm_name, file_path)
      sm_array << new_sm unless new_sm.nil?
      item["supportingMaterials"] = sm_array
      coll.update({"_id" => BSON::ObjectId(@item_id)}, item)
    end

    puts "------ "
    export_json
  end

  def create_sm(name, file_path)

    puts "Create Supporting Material: name: #{name}, file_path: #{file_path}"

    if !File.directory?(file_path) && !can_handle_file(file_path)
      puts "Can't handle file - return nil"
      return nil
    end


    basename = File.basename(name, File.extname(name))
    sm = {
      "name" => basename,
      "files" => []
    }

    if File.directory?(file_path)
      file_list(file_path).each do |f|
        #only set to isMain if no other files are set to isMain and its a html file
        default_files = sm["files"].select{ |ef| ef["isMain"] == true }
        is_main = is_html(f) && default_files.length < 1
        file = create_file(basename, "#{file_path}/#{f}", is_main)
        sm["files"] << file unless file.nil?
      end
    else
      file = create_file(basename, file_path, true)
      sm["files"] << file unless file.nil?
    end
    sm
  end

  def file_list(path)
    Dir.entries(path).select{ |f| f != "." && f != ".." }
  end

  def can_handle_file(file_path)
    can_be_virtual_file(file_path) || can_be_stored_file(file_path)
  end


  def create_file(sm_name, file_path, is_main)
    out = nil
    if can_be_virtual_file(file_path)
      out = create_virtual_file(file_path, is_main)
    elsif can_be_stored_file(file_path)
      storage_key = create_storage_key(sm_name, file_path)
      out = create_stored_file(storage_key, file_path, is_main)
    else
      puts "!!!Don't know how to handle file: #{file_path}/{f}"
    end
    out
  end

  def is_html(name)
    File.extname(name).gsub(".", "") == "html"
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
              .gsub("<", "")
              .gsub(">", "")
              .gsub("/", "")
              .gsub("ﬂ", "fl")
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
    VIRTUAL_FILE_TYPES.include? suffix.downcase
  end

  def can_be_stored_file(filename)
    suffix = File.extname(filename).gsub(".", "")
    STORED_FILE_TYPES.include? suffix.downcase
  end

  def content_type(file_path)
    suffix = File.extname(file_path)
    type = SUFFIX_TO_CONTENT_TYPE_MAP[suffix.downcase.gsub(".", "")]
    type || "unknown"
  end

  def create_stored_file(storage_key,file_path, is_main)

    result = @uploader.store(file_path, storage_key)

    out = {
      "_t" => "models.StoredFile",
      "name" => File.basename(file_path),
      "contentType" => content_type(file_path),
      "isMain" => is_main,
      "storageKey" => storage_key
    }
    out
  end

  def create_virtual_file(file_path, is_main)
    out = {

      "_t" => "models.VirtualFile",
      "name" => File.basename(file_path),
      "contentType" => content_type(file_path),
      "isMain" => is_main,
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

end