# POC Integration

To use this you'll need to have the POC libs published in your local repo..

    git clone git@github.com:corespring/corespring-container.git
    cd corespring-container
    play publish-local


Then run this app and go to:

[http://localhost:9000/poc/5252c7c108738c6d85653725:0/editor.html](http://localhost:9000/poc/5252c7c108738c6d85653725:0/editor.html)

## Components

You'll also need the corespring-components installed:


    git clone git@github.com:corespring/corespring-components.git


By default it looks for them as a sibling project to corespring-api, but you can override this with the CONTAINER_COMPONENTS_PATH env var.