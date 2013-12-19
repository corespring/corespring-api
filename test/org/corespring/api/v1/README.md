# Api Tests

The source for these tests is in the v1-api module.

Normally one would put these tests into the same module, however this isn't working because of how testing is set up.

Also - these tests could also be considered integration tests, and as such having these tests at the root of the project makes sense, whilst unit tests
can be added to the module. Unfortunately it is difficult to write unit tests for these classes at the moment due to the hard wired dependencies.

* TODO: Make a decision on this.
