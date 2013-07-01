#!/usr/bin/env ruby

require_relative '../../../../libs/ruby/quiz-import-export'

=begin
Exports mongo quiz documents as json
also exports the corresponding item sessions

usage:
run.rb db_uri quizId
eg:
run.rb mongodb://localhost:27017/api 000000000000000001
=end



mongo_uri = ARGV[0]
dump_dir = ARGV[1]
puts "running importer"
puts "with: #{mongo_uri}"
puts "and: #{dump_dir}"
importer =  QuizFileImporter.new(mongo_uri, dump_dir)
importer.run
