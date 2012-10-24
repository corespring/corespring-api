//expects vars from and to to have been set in an eval
if( !from || !to ){
    throw "You must specify from and to eg: --eval 'var from = \"dbone\"; var to = \"dbtwo\";'"
}
print("from db: " + from);
print("to db: " + to);

conn = new Mongo("localhost");
var fromDb = conn.getDB(from);
var users = fromDb.users.find();
var toDb = conn.getDB(to);

var fieldValues = {
    "version":"0.0.1",

    "bloomsTaxonomy":[
        {
            "key":"Knowledge (Remember)",
            "value":"Knowledge (Remember)"
        },
        {
            "key":"Understand (Describe, dramatize)",
            "value":"Understand (Describe, dramatize)"
        },
        {
            "key":"Apply",
            "value":"Apply"
        },
        {
            "key":"Analyze",
            "value":"Analyze"
        },
        {
            "key":"Evaluate",
            "value":"Evaluate"
        },
        {
            "key":"Create",
            "value":"Create"
        }
    ],

    "demonstratedKnowledge" : [
        {
            "key" : "Factual",
            "value" : "Factual"
        },
        {
            "key" : "Conceptual",
            "value" : "Conceptual"
        },
        {
            "key" : "Procedural",
            "value" : "Procedural"
        },
        {
            "key" : "Metacognitive",
            "value" : "Metacognitive"
        }
    ],

    "priorUses":[
        {
            "key":"Formative",
            "value":"Formative"
        },
        {
            "key":"Interim",
            "value":"Interim"
        },
        {
            "key":"Benchmark",
            "value":"Benchmark"
        },
        {
            "key":"Summative",
            "value":"Summative"
        },
        {
            "key":"Other",
            "value":"Other"
        }
    ],

    "credentials":[
        {
            "key":"Assessment Developer",
            "value":"Assessment Developer"
        },
        {
            "key":"Test Item Writer",
            "value":"Test Item Writer"
        },
        {
            "key":"State Department of Education",
            "value":"State Department of Education"
        },
        {
            "key":"District Item Writer",
            "value":"District Item Writer"
        },
        {
            "key":"Teacher",
            "value":"Teacher"
        },
        {
            "key":"Student",
            "value":"Student"
        },
        {
            "key":"School Network",
            "value":"School Network"
        },
        {
            "key":"CMO",
            "value":"CMO"
        },
        {
            "key":"Other",
            "value":"Other"
        }
    ],
    "licenseTypes":[
        {
            "key":"CC BY",
            "value":"CC BY"
        },
        {
            "key":"CC BY-SA",
            "value":"CC BY-SA"
        },
        {
            "key":"CC BY-NC",
            "value":"CC BY-NC"
        },
        {
            "key":"CC BY-ND",
            "value":"CC BY-ND"
        },
        {
            "key":"CC BY-NC-SA",
            "value":"CC BY-NC-SA"
        }
    ],
    "itemTypes":[
        {
            "key":"Multiple Choice",
            "value":"Multiple Choice"
        },
        {
            "key":"Text with Questions",
            "value":"Text with Questions"
        },
        {
            "key":"Performance Task",
            "value":"Performance Task"
        },
        {
            "key":"Activity",
            "value":"Activity"
        },
        {
            "key":"Constructed Response - Short Answer",
            "value":"Constructed Response - Short Answer"
        },
        {
            "key":"Constructed Response - Open Ended",
            "value":"Constructed Response - Open Ended"
        },
        {
            "key":"Other (Please Identify)",
            "value":"Other"
        }
    ],

    "gradeLevels":[
        {
            "key":"PK",
            "value":"Prekindergarten"
        },
        {
            "key":"KG",
            "value":"Kindergarten"
        },
        {
            "key":"01",
            "value":"First grade"
        },
        {
            "key":"02",
            "value":"Second grade"
        },
        {
            "key":"03",
            "value":"Third grade"
        },
        {
            "key":"04",
            "value":"Fourth grade"
        },
        {
            "key":"05",
            "value":"Fifth grade"
        },
        {
            "key":"06",
            "value":"Sixth grade"
        },
        {
            "key":"07",
            "value":"Seventh grade"
        },
        {
            "key":"08",
            "value":"Eighth grade"
        },
        {
            "key":"09",
            "value":"Ninth grade"
        },
        {
            "key":"10",
            "value":"Tenth grade"
        },
        {
            "key":"11",
            "value":"Eleventh grade"
        },
        {
            "key":"12",
            "value":"Twelfth grade"
        },
        {
            "key":"13",
            "value":"Grade 13"
        },
        {
            "key":"PS",
            "value":"Postsecondary"
        },
        {
            "key":"AP",
            "value":"Advanced Placement"
        },
        {
            "key":"UG",
            "value":"Ungraded"
        },
        {
            "key":"Other",
            "value":"Other"
        }
    ],

    "reviewsPassed":[
        {
            "key":"Editorial",
            "value":"Editorial"
        },
        {
            "key":"Bias",
            "value":"Bias"
        },
        {
            "key":"Fairness",
            "value":"Fairness"
        },
        {
            "key":"Content",
            "value":"Content"
        },
        {
            "key":"Psychometric",
            "value":"Psychometric"
        },
        {
            "key":"All",
            "value":"All"
        },
        {
            "key":"None",
            "value":"None"
        },
        {
            "key":"Other",
            "value":"Other"
        }
    ],

    "keySkills":[
        {
            "key":"Knowledge",
            "value":["Arrange", "Define", "Describe", "Duplicate",
                "Identify", "Label", "List", "Match", "Memorize", "Name", "Order",
                "Outline", "Recall", "Recognize", "Relate", "Repeat", "Reproduce", "Select", "State"]
        },
        {
            "key":"Understand",
            "value":["Classify", "Convert", "Defend", "Discuss", "Distinguish",
                "Estimate", "Example(s)", "Explain", "Express", "Extend", "Generalize",
                "Give", "Indicate", "Infer", "Locate", "Paraphrase", "Predict", "Review",
                "Rewrite", "Summarize", "Translate", "Understand"]
        },
        {
            "key":"Apply",
            "value":["Apply", "Change", "Choose", "Compute", "Demonstrate", "Discover", "Dramatize",
                "Employ", "Illustrate", "Interpret", "Manipulate", "Modify", "Operate", "Practice",
                "Prepare", "Produce", "Schedule", "Show", "Sketch", "Solve", "Use", "Write"]
        },
        {
            "key":"Analyze",
            "value":["Analyze", "Appraise", "Breakdown", "Calculate", "Categorize", "Compare",
                "Contrast", "Criticize", "Diagram", "Differentiate", "Discriminate", "Examine",
                "Experiment", "Infer", "Model", "Point-Out", "Question", "Separate", "Test"]
        },
        {
            "key":"Evaluate",
            "value":["Assemble", "Collect", "Combine", "Comply", "Devise", "Evaluate",
                "Explain", "Formulate", "Generate", "Plan", "Rearrange"]
        },
        {
            "key":"Create",
            "value":["Create", "Compose", "Construct", "Create", "Design", "Develop"]
        }
    ]
};

toDb.fieldValues.insert(fieldValues);
