![corespring-api commit hooks](logo.jpg)

## corespring-api commit hooks

These commit hooks should be run before every commit. In order to ensure that you have enabled the git pre-commit hooks, please run the following:

    ln -s hooks/pre-commit .git/hooks/pre-commit

### Commit Hook Functions

Below is a list of the current operations performed by the commit hooks

* Run [Scalariform](http://mdr.github.io/scalariform/) to ensure that Scala code is properly formatted.
* Check that there are [no invalid unicode characters](https://www.pivotaltracker.com/story/show/56349810) in *.js files