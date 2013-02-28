web: target/start -Dhttp.port=${PORT} ${JAVA_OPTS} -Dconfig.resource=heroku.conf
worker: java ${JAVA_OPTS} -cp "staged/*" scheduler.RabbitMQ
