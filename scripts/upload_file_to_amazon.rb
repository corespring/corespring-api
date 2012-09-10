#!/usr/bin/ruby

#upload_file_to_amazon.rb

#args: accessKey secretKey bucket_name local_file_path s3_folder
#eg: ruby upload_file_to_amazon.rb AKIAJNPUNTVH2HMFWVVA sl+sXsuq8Xkbl4NvlLuyHRZtrVJp+BXEoH7XlLPm corespring-assets /Users/edeustace/Desktop/cute-puppy.jpg test_images


require 'aws/s3'

access_key = ARGV[0]
secret_key = ARGV[1]
bucket_name = ARGV[2]
file_path = ARGV[3]
s3_folder = ARGV[4]




AWS::S3::Base.establish_connection!(
    :access_key_id     => access_key,
    :secret_access_key => secret_key
  )


AWS::S3::Service.buckets.each do |b|

  puts b.name
end

basename = File.basename(file_path)
s3_name = "#{s3_folder}/#{basename}"
puts "putting: #{s3_name} into #{bucket_name}"
AWS::S3::S3Object.store( s3_name, open(file_path), bucket_name)


bucket = AWS::S3::Bucket.find(bucket_name)
result = bucket[s3_name]
puts "successfully stored to: #{result.key}"