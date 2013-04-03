class MongoToolsException < RuntimeError
  attr :cmd, :output
  def initialize(cmd, output)
    @cmd = cmd 
    @output =output 
  end
end


class MongoTools

  def self.export(host,port,db,collection,query,output,username="",password="", opts = {})
    options = []
    options << o("--host", "#{host}:#{port}")
    options << o("-d", db)
    options << o("-c", collection)
    options << o("--query", "'#{query}'")
    options << o("-o", output)
    options << o("-u", username)
    options << o("-p", password)
    puts ">>>>>>> opts: #{opts}"
    options << o("--jsonArray") if opts[:json_array]

    puts options
    cmd = "mongoexport "

    options.each do |o|
      puts o.to_cmd
      puts o.empty?
      cmd << "#{o.to_cmd} " unless o.empty?
    end
    puts "MongoTools::export - run: #{cmd}"
    output = `#{cmd}`
    raise MongoToolsException.new("#{cmd}", output) unless $?.to_i == 0

  end

  def self.import(host,port,db,collection,file,username="",password="", opts = {})
    options = []
    options << o("--host", "#{host}:#{port}")
    options << o("-d", db)
    options << o("-c", collection)
    options << o("--file", file)
    options << o("-u", username)
    options << o("-p", password)
    options << o("--jsonArray") if opts[:json_array]
    cmd = "mongoimport "

    puts options
    options.each do |o|
      cmd << "#{o.to_cmd} " unless o.empty?
    end
    puts "MongoTools::import - run: #{cmd}"
    output = `#{cmd}`
    raise MongoToolsException.new("#{cmd}", output) unless $?.to_i == 0
  end


  # wrapper for monogdump shell command
  def self.dump(host,port,db,output,username = "", password = "")

    options = []
    options << o("-h", "#{host}:#{port}")
    options << o("-db", db)
    options << o("-o", output)
    options << o("-u", username)
    options << o("-p", password)

    cmd = "mongodump "

    options.each do |o|
      cmd << "#{o.to_cmd} " unless o.empty?
    end
    puts "MongoTools::dump - run: #{cmd}"
    output = `#{cmd}`
    raise MongoToolsException.new("#{cmd}", output) unless $?.to_i == 0
  end

  # wrapper for mongorestore shell command
  def self.restore(host,port,db,source_folder,username = "", password = "")

    options = []
    options << o("-h", "#{host}:#{port}")
    options << o("-db", db)
    options << o("-u", username)
    options << o("-p", password)

    cmd = "mongorestore "

    options.each do |o|
      cmd << "#{o.key} #{o.value} " unless o.empty?
    end
    #ensure that we drop everything before we restore.
    cmd << "--drop "
    cmd << "#{source_folder}"
    output = `#{cmd}`
    raise MongoToolsException.new("#{cmd}", output) unless $?.to_i == 0
  end

  private 
  def self.o(key,value = nil)
    if !value.nil? 
      Option.new(key,value)
    else 
      SingleOption.new(key)
    end
  end

end

class SingleOption
  def initialize(key)
    @key = key
  end

  def to_cmd
    @key
  end

  def to_s
    "[SingleOption[#{@key}]]"
  end

  def empty?
    @key.empty? || @key.nil?
  end
end

class Option
  attr_accessor :key, :value

  def initialize(key,value = nil)
    @key = key
    @value = value
  end

  def to_cmd
    out = "#{@key}"
    out += " #{@value}" unless @value.nil?
    out
  end

  def to_s
    "[Option[key: #{@key}, value: #{@value}]]"
  end 

  def empty?
    @value.nil? || @value.empty?
  end
end

