# Integration Tests

If you want to write a test that contains collabarating units with IO,
then this is the place to add them.
These tests are run sequentially and not forked.

### Commands

    it:compile

    it:test

    it:test-only ...


### Seeing the logs

    play -Dlogger.resource=logging/it-logger.xml
