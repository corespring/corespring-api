puts "This is just a small test to check the behaviour of the amazon uploader"

load './amazon_uploader.rb'


uploader = AmazonUploader.new( "AKIAJNPUNTVH2HMFWVVA", "sl+sXsuq8Xkbl4NvlLuyHRZtrVJp+BXEoH7XlLPm", "corespring-assets")

local_file = '../../../corespring-content/Beta Prep/5023cbb6e4b08be40d045d58/ScoringGuide/4pointb.png'
s3_name = '5023cbb6e4b08be40d045d58/materials/ScoringGuide/4pointb.png'


is_newer = uploader.local_file_newer_than_stored_file?(local_file, s3_name)

puts is_newer



is_newer = uploader.local_file_newer_than_stored_file?(local_file, 'adfasdf')

puts is_newer


