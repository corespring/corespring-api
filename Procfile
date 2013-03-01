web: target/start -Dhttp.port=${PORT} ${JAVA_OPTS} -Dconfig.resource=heroku.conf
worker: java ${JAVA_OPTS} -cp "target/staged/*" scheduler.RabbitMQ -Dconfig.resource=heroku.conf