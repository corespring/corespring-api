require 'fileutils'
require 'json'
require 'mongo-db-utils/version'
require 'mongo-db-utils/models/db'
require 'mongo-db-utils/tools/commands'

raise "wrong version" if MongoDbUtils::VERSION != "0.1.1"

include MongoDbUtils::Tools


class QuizFileExporter
  def initialize(mongo_uri, ids, out_path)
    @db = MongoDbUtils::Model.db_from_uri(mongo_uri)
    @ids = ids
    @out_path = out_path
  end

  def run

    puts "running QuizFileExporter..."
    @ids.each{ |id|
      exported_quiz_path = export_quiz(id)
      json = get_json(exported_quiz_path)
      export_item_sessions(id,json)
    }

  end

  private

  def export_quiz(id)
    FileUtils.remove_dir(@out_path) if File.exists?(@out_path)
    query = id_query(id)
    quiz_out = "#{@out_path}/quiz.json"
    run_export("quizzes", query, quiz_out)
    quiz_out
  end

  def export_item_sessions(id, json)

    session_ids = []

    json["participants"].each{ |p|
      puts ">> p: #{p}"

      p["answers"].each{ |a|
        puts ">>> a: #{a}"
        id = a["sessionId"]["$oid"]
        puts "!! id: #{id}"
        session_ids << id
      }
    }

    export_session(@out_path, session_ids)
  end

  def export_session(root_path, ids)
    opts = { :json_array => true }
    run_export("itemsessions", multiple_ids_query(ids), "#{root_path}/itemsessions.json", opts)
  end


  def get_json(path)
    file = File.open(path, "rb")
    contents = file.read
    JSON.parse(contents)
  end


  def id_query(id)
    "{ _id : ObjectId(\"#{id}\") }"
  end

  def multiple_ids_query(ids)
    oids = ids.map{ |id| "ObjectId(\"#{id}\")"}.join(",")
    query = "{ _id : { $in: [#{oids}] }}"
    puts "query: #{query}"
    query
  end

  def run_export(coll,query,out,opts = {})
    Export.new(@db.to_host_s,@db.name,coll,query,out,@db.username,@db.password, opts).run
  end

end

class QuizFileImporter
  def initialize(mongo_uri, dump_dir)
    @db = MongoDbUtils::Model.db_from_uri(mongo_uri)
    puts "@db: #{@db}"
    @dump_dir = dump_dir
  end

  def run
    Dir["#{@dump_dir}/*"].each{ |p|
        puts "p: #{p}"
        import_quiz( "#{p}/quiz.json", "quizzes")
        import_sessions("#{p}/itemsessions.json", "itemsessions")
    }
  end

  private

  def import_sessions(json, collection)
    Import.new(@db.to_host_s,@db.name,collection,json,@db.username,@db.password, {:json_array => true}).run
  end

  def import_quiz(json, collection)
    "db: ---------> #{@db}"
    Import.new(@db.to_host_s,@db.name,collection,json,@db.username,@db.password).run
  end
end
