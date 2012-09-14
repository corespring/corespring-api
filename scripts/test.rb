test = <<-EOF 
 "title":"An appliance company builds washing machines and dishwashers. The company has at most 20,000 hours of labor available each month. Write a system of inequalities and answer some questions about manufacturing capacity.""
EOF

r1 = "".match(/"title".*?:.*?"(.*?\s+.*?\s+.*?)\s+/m)
puts r1.nil?
r2 = test.match(/"title".*?:.*?"(.*?\s+.*?\s+.*?)\s+/m)
puts r2.length

t = r2[1]

t.gsub!(" ", "-")
puts t