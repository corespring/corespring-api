#! /bin/sh
if [ -z "$1" ]; then
    echo "you must supply a task to execute"
else
    cd tasks/$1
    if [ "$?" = "0" ]; then
        sbt run
    else
        echo "error: task $1 not run"
    fi
fi