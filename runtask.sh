#! /bin/sh
if [ -z "$1" ]; then
    echo "you must supply a task to execute"
else
    #change directory to the task given then run sbt
    cd tasks/$1
    if [ "$?" = "0" ]; then
        command -v sbt
        if [ "$?" = "0" ]; then
            sbt run
        else
            #HEROKU FIX: check if the path to sbt is .sbt_home/bin. if it is, add ~/.sbt_home/bin to path to make sbt executable from any folder
            command -v ~/.sbt_home/bin/sbt;
            if [ "$?" = "0" ]; then
                ~/.sbt_home/bin/sbt run
            else
                echo "error: could not run sbt. sbt command unavailable"
            fi
        fi
    else
        echo "error: task $1 not run because the directory tasks/$1 could not be found"
    fi
fi