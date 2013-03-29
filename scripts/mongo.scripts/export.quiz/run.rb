#!/usr/bin/env ruby

require '../../../deployment/libs/ruby/db'
require '../../../deployment/libs/ruby/mongo_tools'
require 'json'
require 'fileutils'


def id_query(id)
  "{ _id : ObjectId(\"#{id}\") }"
end

def run_export(db,coll,query,out)
  puts db
  MongoTools.export(db.host,db.port,db.name,coll,query,out,db.username,db.password)
end

def export_session(db,root_path, id)
  FileUtils.mkdir("#{root_path}/itemsessions") unless File.exists?("#{root_path}/itemsessions")
  run_export(db, "itemsessions", id_query(id), "#{root_path}/itemsessions/#{id}.json")
end


def get_json(path)
  file = File.open(path, "rb")
  contents = file.read
  JSON.parse(contents)
end

def export_quiz(db,root_path,id)
  FileUtils.remove_dir(root_path) if File.exists?(root_path)
  query = id_query(id)
  quiz_out = "#{root_path}/quizzes/#{id}.json"
  FileUtils.mkdir_p("#{root_path}/quizzes")
  run_export(db, "quizzes", query, quiz_out)
  quiz_json = get_json(quiz_out)

  quiz_json["participants"].each{ |p| 
    puts ">> p: #{p}"

    p["answers"].each{ |a| 
      puts ">>> a: #{a}"
      id = a["sessionId"]["$oid"]
      puts "!! id: #{id}"
      export_session( db, root_path, id  )
    }
  }
end

def format_json(dir)
  Dir["#{dir}/**/*.json"].each{ |path|
     puts "path: #{path}"
     json = get_json(path)
     File.open(path, 'w') { |file|
       pretty = JSON.pretty_generate(json) 
       file.write(pretty) 
     }
  }
end



mongo_uri = ARGV[0]
puts "db: #{Db}"
db = Db.from_uri(mongo_uri)
puts "now: #{db}"
quiz_id = ARGV[1]
out_dir = "QUIZ_#{quiz_id}"
export_quiz(db,out_dir, quiz_id)
format_json(out_dir)
