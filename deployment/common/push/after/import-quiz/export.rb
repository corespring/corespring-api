#!/usr/bin/env ruby

require_relative '../../../../libs/ruby/quiz-import-export'

=begin 
Exports mongo quiz documents as json
also exports the corresponding item sessions

usage: 
run.rb db_uri quizId path_to_dump_to
eg:
run.rb mongodb://localhost:27017/api 000000000000000001 dump
=end

require 'json'

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

def get_json(path)
  file = File.open(path, "rb")
  contents = file.read
  JSON.parse(contents)
end


mongo_uri = ARGV[0]
puts "db: #{Db}"
quiz_id = ARGV[1]
out = ARGV[2] || "."
out_dir = "#{out}/#{quiz_id}"
exporter =  QuizFileExporter.new(mongo_uri,[quiz_id], out_dir)
exporter.run
# Pretty print the json
#format_json(out_dir)