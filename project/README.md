# Corespring Build Structure

The build file defines the build for this project.

The project is broken down into a set of libraries and play modules.

The purpose of this is:
* To provide reusable libraries
* To define clear boundaries between interacting parts of the system
* To provide a clear api between libraries and to provide encapsulation

The libraries and modules are located in the modules folder:
* lib - for libraries - vanilla sbt projects
* test-lib - for test libraries used only for testing
* web - for play libraries


## Libraries

* core - The core library (Note: The intention is to further break this library down to data-models, data-service, data-json)
* assets - A library that contains an interface to S3 and working with assets
* common-utils - any shared utils
* player-lib - some player library behaviour
* qti - the qti parser and logic

## Play Modules

* common-views - some shared html templates and view helpers
* public - the public section of the site

## Test Libraries

* test-helpers - any shared helper libraries for any testing that needs to be done

## Testing

Note: The seeding of the app is now part of the sbt build - It won't happen as part of the app start up. To seed before you test run:

    > seed-test
    > test


## Using the sbt command line
* `project corespring-public` - set 'corespring-public' as the current build target this allows you to compile/test only this project
* `project <tab>` - to get a list of available projects

## General Rules/Thoughts

* all packages must reside under `org.corespring`
* the next package(s) under `org.corespring` must give a clear indication of the functional area that the package provides: eg: `org.corepsring.common.utils`
* place unit tests in the same project as the behaviour - in the exact same namespace
* try to place new behaviour in the most appropriate library - if it doesn't exisit and the behaviour is substantial enough consider creating a library for it
* when creating new libraries try to minimize its dependencies as much as possible (atm too many libraries depend on `core`, which in turn depends on too many libraries)
* if a library is depended on by too many others it is an indication that that library could be further divided in to sub libraries
* to help minimize dependencies and make a library, reusable try and work with abstractions
* we may at some point want to consider moving some of the libraries out of this project altogether and make them dependencies


#### Thoughts on further modularization.

##### Core => models, json + services

models - only the case classes
json - only the json serialization of the models
services - the service behaviour for the given models

One thing that is nice about json serialization at the moment is that the Reads/Writes are picked up automatically as they are defined in the model companion objects. So for example

    class MyController{
      def get = Action{ request => Ok( Json.toJson(MyModel())) }
    }

In this the Json Writes for type MyModel will be automatically picked up because the implicit look up behaviour will check the companion object of MyModel.

The bad thing about this is that you need to pollute your class with a json implementation - not something that your model cares about.

You could have a model companion in a project where everything is defined??
