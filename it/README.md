# Integration Tests

If you want to write a test that contains collabarating units with IO,
then this is the place to add them.
These tests are run sequentially and not forked.

### Commands

    it:compile

    it:test

    it:test-only ...


### Seeing the logs

    play -Dlogger.resource=conf/logging/it-logger.xml


### Integration test top tips

* The tests are run sequentially and are not forked.
* Be wary of play's route helper method see: https://gist.github.com/edeustace/972f78ef8238143c91ca

